package com.worker;

import com.handler.RequestHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Data
public class NioSelectorWorker implements Runnable {

    private final Selector selector;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingWrites = new ConcurrentHashMap<>();
    private final String name;
    private final RequestHandler handler;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @FunctionalInterface
    public interface ReadHandler {
        void handleRead(SocketChannel channel) throws IOException;
    }

    @FunctionalInterface
    public interface WriteHandler {
        void handleWrite(SocketChannel channel) throws IOException;
    }

    public static class Callbacks {
        public final ReadHandler onRead;
        public final WriteHandler onWrite;

        public Callbacks(ReadHandler r, WriteHandler w) {
            this.onRead = r;
            this.onWrite = w;
        }
    }

    public NioSelectorWorker(String name, RequestHandler handler) {
        this.name = name;
        this.handler = handler;
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("启动worker的selector失败", e);
        }
    }

    public void register(SocketChannel channel, int ops, ReadHandler readHandler, WriteHandler writeHandler) {
        taskQueue.add(() -> {
            try {
                SelectionKey key = channel.register(selector, ops);
                key.attach(new Callbacks(readHandler, writeHandler));
            } catch (Exception e) {
                log.error("通道注册失败", e);
            }
        });
        selector.wakeup();
    }

    public void register(SocketChannel channel, int ops, ReadHandler readHandler) {
        register(channel, ops, readHandler, this::defaultWrite);
    }

    public void register(SocketChannel channel, int ops) {
        register(channel, ops, this::serverRead, this::defaultWrite);
    }

    @Override
    public void run() {
        log.info("Worker [{}] 启动.", name);
        while (true) {
            try {
                selector.select();
                Runnable task;
                while ((task = taskQueue.poll()) != null) {
                    task.run();
                }

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (!key.isValid()) continue;
                    SocketChannel channel = (SocketChannel) key.channel();
                    Callbacks cbs = (Callbacks) key.attachment();

                    try {
                        if (key.isReadable()) {
                            cbs.onRead.handleRead(channel);
                        }
                        if (key.isWritable()) {
                            cbs.onWrite.handleWrite(channel);
                        }
                    } catch (IOException e) {
                        log.error("通道读写异常，关闭连接", e);
                        key.cancel();
                        try {
                            channel.close();
                        } catch (IOException ignored) {}
                    }
                }
            } catch (IOException e) {
                log.error("Selector 轮询异常", e);
            }
        }
    }

    public void serverRead(SocketChannel channel) throws IOException {
        ByteBuffer lenBuf = ByteBuffer.allocate(4);
        if (channel.read(lenBuf) < 4) return;
        lenBuf.flip();
        int len = lenBuf.getInt();
        if (len > 1024 * 1024) throw new RuntimeException("消息过大");
        ByteBuffer dataBuf = ByteBuffer.allocate(len);
        while (dataBuf.hasRemaining()) {
            if (channel.read(dataBuf) == -1) throw new IOException("连接关闭");
        }
        dataBuf.flip();
        byte[] data = new byte[dataBuf.remaining()];
        dataBuf.get(data);
        log.info("Worker [{}] 收到数据：{}", name, new String(data));

        executor.execute(() -> {
            byte[] response = handler.onRequest(new ByteArrayInputStream(data));
            ByteBuffer respBuf = ByteBuffer.allocate(4 + response.length);
            respBuf.putInt(response.length).put(response).flip();
            writeData(channel, respBuf);
        });
    }

    public void defaultWrite(SocketChannel channel) throws IOException {
        Queue<ByteBuffer> queue = pendingWrites.get(channel);
        while (queue != null && !queue.isEmpty()) {
            ByteBuffer buffer = queue.peek();
            channel.write(buffer);
            if (buffer.hasRemaining()) return;
            queue.poll();
        }
        SelectionKey key = channel.keyFor(selector);
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    public void writeData(SocketChannel channel, ByteBuffer data) {
        taskQueue.add(() -> {
            try {
                int written = channel.write(data);
                if (data.hasRemaining()) {
                    pendingWrites.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>()).offer(data);
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null && key.isValid()) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    }
                }
            } catch (IOException e) {
                log.error("写数据失败", e);
                try {
                    channel.close();
                } catch (IOException ignored) {}
            }
        });
        selector.wakeup();
    }
    public void clientRead(SocketChannel channel, ClientContext context) {
        try {
            if (context.readingLen) {
                // 读取长度
                if (channel.read(context.getLenBuffer()) == -1) {
                    channel.close();
                    context.getFuture().completeExceptionally(new IOException("channel closed"));
                    return;
                }

                if (context.getLenBuffer().hasRemaining()) {
                    return; // 继续等待下次读
                }

                context.getLenBuffer().flip();
                int len = context.getLenBuffer().getInt();

                if (len > 1024 * 1024) {
                    context.getFuture().completeExceptionally(new RuntimeException("粘包或数据过大"));
                    channel.close();
                    return;
                }

                context.setDataBuffer(ByteBuffer.allocate(len));
                context.setReadingLen(false); // 转入数据读取阶段
            }
            ByteBuffer dataBuffer = context.getDataBuffer();
            if (channel.read(dataBuffer) == -1) {
                channel.close();
                context.getFuture().completeExceptionally(new IOException("channel closed"));
                return;
            }

            if (dataBuffer.hasRemaining()) {
                log.info("还有数据");
                return; // 继续等待
            }

            // 读取完成，处理数据
            dataBuffer.flip();
            byte[] data = new byte[dataBuffer.remaining()];
            dataBuffer.get(data);

            ByteArrayInputStream result = new ByteArrayInputStream(data);
            context.getFuture().complete(result);
            log.info("clientRead end");
            SelectionKey key = channel.keyFor(selector);
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() & ~ SelectionKey.OP_READ);
            }
            // 可选：归还连接池
            // connectionPool.returnChannel(channel);

        } catch (IOException e) {
            try {
                channel.close();
            } catch (IOException ignored) {}
            context.getFuture().completeExceptionally(e);
        }
    }


}





