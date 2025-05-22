package com.worker;

import com.handler.RequestHandler;
import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Data
public class NioSelectorWorker implements Runnable {
    private final Selector selector;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final Map<SocketChannel, Queue<ByteBuffer>> pendingWrites = new ConcurrentHashMap<>();
    private final String name;
    private RequestHandler handler;

    public NioSelectorWorker(String name, RequestHandler handler) {
        this.name = name;
        this.handler = handler;
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("启动worker的selector失败", e);
        }
    }

    public void register(SocketChannel channel, int ops) {
        taskQueue.add(() -> {
            try {
                channel.register(selector, ops);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        selector.wakeup(); // 唤醒 selector，防止 select() 阻塞中
    }

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
                    if (key.isReadable()) {
                        handleRead(channel);
                    }else if (key.isWritable()) {
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

                        // 如果已经写完所有，取消写事件监听
                        if (queue == null || queue.isEmpty()) {
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void handleRead(SocketChannel channel) {
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
            // 模拟处理请求
            System.out.println(name + " 收到数据：" + new String(data));
            //写回数据
            // 新的逻辑：调用 handler 获取响应数据
            byte[] resp = handler.onRequest(new ByteArrayInputStream(data));

            // 构建响应 buffer
            ByteBuffer respBuf = ByteBuffer.allocate(4 + resp.length);
            respBuf.putInt(resp.length);
            respBuf.put(resp);
            respBuf.flip();
            handlerWrite(channel, respBuf);

        } catch (IOException e) {
            try {
                channel.close();
            } catch (IOException ignored) {}
        }
    }
    public void handlerWrite(SocketChannel channel, ByteBuffer data) {
        pendingWrites.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>()).offer(data);
        // 触发写事件注册（需要线程安全处理）
        selector.wakeup(); // 唤醒 select()
        SelectionKey key = channel.keyFor(selector);
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }
}
