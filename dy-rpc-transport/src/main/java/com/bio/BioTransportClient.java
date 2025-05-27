package com.bio;

import com.protocol.Peer;
import com.service.TransportClient;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class BioTransportClient implements TransportClient {
    @Override
    public CompletableFuture<InputStream> sendAsync(InputStream data) {
        return null;
    }

    @Override
    public void init(Peer peer) {

    }

    private String host;
    private int port;
    private Socket socket;

    public BioTransportClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.socket = new Socket(host, port);
    }

    @Override
    public InputStream write(InputStream data) {
        try {
            OutputStream out = socket.getOutputStream();

            // 把请求数据写到socket输出流
            data.transferTo(out);
            out.flush();

            // 关键：告诉服务端请求发送完毕了
            socket.shutdownOutput();

            // 接收响应
            InputStream in = socket.getInputStream();

            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            in.transferTo(responseBuffer);

            return new ByteArrayInputStream(responseBuffer.toByteArray());

        } catch (IOException e) {
            System.out.println("Client write error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Client close failed", e);
        }
    }
}
