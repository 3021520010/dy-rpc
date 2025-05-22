package com.bio;


import com.handler.RequestHandler;
import com.service.TransportServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

public class TransportServerDemo {
    public static void main(String[] args) throws Exception {
        // 启动服务端
        TransportServer server = new BioTransportServer(3000);
        RequestHandler requestHandler=new RequestHandler() {
            @Override
            public void onRequest(InputStream request, OutputStream response) {
                try {
                    String msg = new String(request.readAllBytes());
                    String reply = "Server echo: " + msg;
                    response.write(reply.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRequest(InputStream request, SocketChannel channel) {

            }

            @Override
            public byte[] onRequest(InputStream input) {
                return new byte[0];
            }
        };
        server.init(3000, requestHandler);
        server.start();
        //new Thread(server::start).start();


        //server.stop();
    }
}
