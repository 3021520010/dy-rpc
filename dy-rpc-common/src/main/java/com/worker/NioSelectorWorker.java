package com.worker;

import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Data
public class NioSelectorWorker implements Runnable {
    private final Selector selector;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final String name;

    public NioSelectorWorker(String name) {
        this.name = name;
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open selector", e);
        }
    }

    public void register(SocketChannel channel, int ops, Runnable callback) {
        taskQueue.add(() -> {
            try {
                SelectionKey key = channel.register(selector, ops);
                key.attach(callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        selector.wakeup(); // 唤醒 selector，防止 select() 阻塞中
    }

    @Override
    public void run() {
        System.out.println(name + " started.");
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

                    Runnable callback = (Runnable) key.attachment();
                    if (callback != null) {
                        callback.run();
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

            // TODO: 你可以在这里调用 handler.onRequest(new ByteArrayInputStream(data), channel);
        } catch (IOException e) {
            try {
                channel.close();
            } catch (IOException ignored) {}
        }
    }
}
