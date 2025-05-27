package com.worker;

import com.handler.RequestHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Data
public class NioSelectorWorker implements Runnable {

    private final Selector selector;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingWrites = new ConcurrentHashMap<>();
    private final String name;
    private RequestHandler handler;


    // 函数式接口，定义读写处理器
    @FunctionalInterface
    public interface ReadHandler {
        void handleRead(SocketChannel channel) throws IOException;
    }

    @FunctionalInterface
    public interface WriteHandler {
        void handleWrite(SocketChannel channel) throws IOException;
    }

    // 挂载类，保存读写回调
    public static class Callbacks {
        public ReadHandler onRead;
        public WriteHandler onWrite;
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

    // 注册通道时传入读写回调
    public void register(SocketChannel channel, int ops, ReadHandler readHandler, WriteHandler writeHandler) {
        taskQueue.add(() -> {
            try {
                SelectionKey key = channel.register(selector, ops);
                key.attach(new Callbacks(readHandler, writeHandler));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        selector.wakeup();
    }
    public void register(SocketChannel channel, int ops, ReadHandler readHandler) {
       register(channel, ops, readHandler, null);
    }
    public void register(SocketChannel channel, int ops, WriteHandler writeHandler) {
        register(channel, ops, null, writeHandler);
    }
    public void register(SocketChannel channel, int ops) {
        register(channel, ops, null, null);
    }
    // 支持读、写、或读+写的任意组合注册

    @Override
    public void run() {
        System.out.println(name + " 启动.");
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
                    SocketChannel channel = (SocketChannel) key.channel();
                    Object attachment = key.attachment();

                    if (key.isReadable()) {
                        if (attachment instanceof Callbacks) {
                            Callbacks cbs = (Callbacks) attachment;
                            if (cbs.onRead != null) {
                                cbs.onRead.handleRead(channel);
                            } else {
                                serverRead(channel);
                            }
                        } else {
                            serverRead(channel);
                        }
                    }

                    if (key.isWritable()) {
                        if (attachment instanceof Callbacks) {
                            Callbacks cbs = (Callbacks) attachment;
                            if (cbs.onWrite != null) {
                                cbs.onWrite.handleWrite(channel);
                            } else {
                                defaultWrite(channel, key);
                            }
                        } else {
                            defaultWrite(channel, key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 你的默认读处理逻辑
    public void serverRead(SocketChannel channel) {
        try {
            ByteBuffer lenBuf = ByteBuffer.allocate(4);
            int read = channel.read(lenBuf);
            if (read == -1) {
                channel.close();
                return;
            }
            while (lenBuf.hasRemaining()) {
                if (channel.read(lenBuf) == -1) {
                    channel.close();
                    return;
                }
            }
            lenBuf.flip();
            int dataLen = lenBuf.getInt();
            if (dataLen > 1024 * 1024) {
                throw new RuntimeException("server读的数据过大");
            }
            ByteBuffer dataBuf = ByteBuffer.allocate(dataLen);
            while (dataBuf.hasRemaining()) {
                if (channel.read(dataBuf) == -1) {
                    channel.close();
                    return;
                }
            }

            dataBuf.flip();
            byte[] data = new byte[dataBuf.remaining()];
            dataBuf.get(data);
            System.out.println(name + " 收到数据：" + new String(data));

            byte[] resp = handler.onRequest(new ByteArrayInputStream(data));
            System.out.println(name + " 写回数据：" + new String(resp));
            if (resp.length > 1024 * 1024) {
                throw new RuntimeException("server写的数据过大");
            }
            ByteBuffer respBuf = ByteBuffer.allocate(4 + resp.length);
            respBuf.putInt(resp.length);
            respBuf.put(resp);
            respBuf.flip();

            writeData(channel, respBuf);

        } catch (IOException e) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    // 你的默认写逻辑
    public void defaultWrite(SocketChannel channel, SelectionKey key) throws IOException {
        log.info("defaultWrite start");
        Queue<ByteBuffer> queue = pendingWrites.get(channel);
        while (queue != null && !queue.isEmpty()) {
            ByteBuffer buffer = queue.peek();
            channel.write(buffer);
            if (buffer.hasRemaining()) {
                // 没写完，下次继续写
                break;
            }
            queue.poll(); // 写完移除
        }
        // 如果写完所有数据，取消写事件监听
        if (queue == null || queue.isEmpty()) {
            log.info("defaultWrite end");
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    // 写回数据的辅助方法，跟原来一样
    public void writeData(SocketChannel channel, ByteBuffer data) {
        try {
            int written = channel.write(data);
            if (data.hasRemaining()) {
                // 没写完，放入队列，注册 OP_WRITE
                pendingWrites.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>()).offer(data);
                SelectionKey key = channel.keyFor(selector);
                if (key != null && key.isValid()) {
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
            } else {
                log.info("数据已一次性写完");
            }
        } catch (IOException e) {
            log.error("写数据失败", e);
        }
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
