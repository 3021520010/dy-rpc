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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Data
public class NioSelectorWorker implements Runnable {
    private final Selector selector;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
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
            handler.onRequest(new ByteArrayInputStream(data), channel);
        } catch (IOException e) {
            try {
                channel.close();
            } catch (IOException ignored) {}
        }
    }
}
