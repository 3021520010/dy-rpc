package com.worker;

import com.handler.RequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

public class BossServer implements Runnable {
    private final int port;
    private final NioSelectorWorker[] workers;
    private int workerIndex = 0;
    private final RequestHandler handler;
    private Selector selector = null;
    private static volatile BossServer instance;

    private BossServer(int port, int workerCount, RequestHandler requestHandler) {
        this.port = port;
        this.handler = requestHandler;
        this.workers = new NioSelectorWorker[workerCount];
        try {
            selector=Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new NioSelectorWorker("Worker-" + i, handler);
            new Thread(workers[i]).start();
        }
    }
    public Selector getSelector() {
        return selector;
    }
    /**
     * 获取单例实例
     */
    public static BossServer getInstance(int port, int workerCount, RequestHandler handler) {
        if (instance == null) {
            synchronized (BossServer.class) {
                if (instance == null) {
                    instance = new BossServer(port, workerCount, handler);
                }
            }
        }
        return instance;
    }
    public static BossServer getInstance() {
        if (instance != null) {
            return instance;
        }
        return null;
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);

            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Boss线程启动，监听 " + port);

            while (true) {
                selector.select();

                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        System.out.println("建立连接: " + clientChannel.getRemoteAddress());

                        // Round-robin 分发给 worker
                        NioSelectorWorker worker = workers[workerIndex++ % workers.length];
                        worker.register(clientChannel, SelectionKey.OP_READ);
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
