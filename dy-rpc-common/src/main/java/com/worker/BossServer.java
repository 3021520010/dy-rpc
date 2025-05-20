package com.worker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

public class BossServer implements Runnable {
    private final int port;
    private final NioSelectorWorker[] workers;
    private int workerIndex = 0;

    public BossServer(int port, int workerCount) {
        this.port = port;
        this.workers = new NioSelectorWorker[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new NioSelectorWorker("Worker-" + i);
            new Thread(workers[i]).start();
        }
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Boss started on port " + port);

            while (true) {
                selector.select();

                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        System.out.println("Accepted connection from " + clientChannel.getRemoteAddress());

                        // Round-robin 分发给 worker
                        NioSelectorWorker worker = workers[workerIndex++ % workers.length];
                        worker.register(clientChannel, SelectionKey.OP_READ, () -> {
                            worker.handleRead(clientChannel);
                        });
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
