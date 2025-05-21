package com.nio;

import com.handler.RequestHandler;
import com.service.TransportServer;
import com.worker.BossServer;

public class NioTransportServer implements TransportServer {
    private int port;
    private RequestHandler handler;
    @Override
    public void init(int port, RequestHandler requestHandler) {
        this.handler = requestHandler;
        this.port = port;

    }
    @Override
    public void start() {
        BossServer boss = BossServer.getInstance(port, 2, handler);
        new Thread(boss).start();
    }

    @Override
    public void stop() {

    }
}
