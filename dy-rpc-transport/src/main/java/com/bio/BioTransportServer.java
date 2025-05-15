package com.bio;


import com.service.RequestHandler;
import com.service.TransportServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BioTransportServer implements TransportServer {

    private int port;
    private ServerSocket serverSocket;
    private RequestHandler handler;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private volatile boolean running = false;

    public BioTransportServer(int port) {
        this.port = port;
    }

    @Override
    public void init(int port, RequestHandler requestHandler) {
            this.handler = requestHandler;
            this.port = port;
    }
    @Override
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port " + port);

            while (running) {
                Socket client = serverSocket.accept();
                threadPool.execute(() -> handleClient(client));
            }

        } catch (IOException e) {
            throw new RuntimeException("Server start failed", e);
        }
    }

    private void handleClient(Socket client) {
        try (InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream()) {
            handler.onRequest(in, out);
            out.flush();
        } catch (IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException ignore) {}
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            serverSocket.close();
            threadPool.shutdown();
        } catch (IOException e) {
            throw new RuntimeException("Server stop failed", e);
        }
    }
}