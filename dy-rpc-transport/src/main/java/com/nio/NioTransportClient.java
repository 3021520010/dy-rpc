package com.nio;

import com.connection.NIOConnectionPool;
import com.protocol.Peer;
import com.service.TransportClient;
import com.worker.NioSelectorWorker;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioTransportClient implements TransportClient {

   private Peer peer;
   private NIOConnectionPool nioConnectionPool = NIOConnectionPool.getNIOConnectionPool();
    private SocketChannel channel;
    public NioTransportClient() {

    }
    public void init(Peer peer){
        this.peer = peer;
        nioConnectionPool.initConnections(peer);
    }
    @Override
    public InputStream write(InputStream data) {
        try {
            channel=nioConnectionPool.getConnection(peer);
            // 1. 先将输入流内容读取为byte[]
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            data.transferTo(bos);
            byte[] body = bos.toByteArray();

            // 2. 构造写入缓冲区（4字节长度+数据）
            ByteBuffer writeBuffer = ByteBuffer.allocate(4 + body.length);
            writeBuffer.putInt(body.length);
            writeBuffer.put(body);
            writeBuffer.flip();

            // 3. 循环写入，直到写完
            while (writeBuffer.hasRemaining()) {
                int written = channel.write(writeBuffer);
                if (written == 0) {
                    // 写不出去时，可以稍作等待或调用selector进行写事件监听，但这里简单睡一下
                    Thread.sleep(10);
                }
            }

            // 4. 读取响应长度（4字节）
            ByteBuffer lenBuffer = ByteBuffer.allocate(4);
            while (lenBuffer.hasRemaining()) {
                int read = channel.read(lenBuffer);
                if (read == -1) {
                    throw new IOException("服务器关闭连接");
                }
                if (read == 0) {
                    Thread.sleep(10);
                }
            }
            lenBuffer.flip();
            int respLen = lenBuffer.getInt();

            // 5. 读取响应数据
            ByteBuffer respBuffer = ByteBuffer.allocate(respLen);
            while (respBuffer.hasRemaining()) {
                int read = channel.read(respBuffer);
                if (read == -1) {
                    throw new IOException("服务器关闭连接");
                }
                if (read == 0) {
                    Thread.sleep(10);
                }
            }
            respBuffer.flip();

            // 6. 返回响应数据的InputStream
            byte[] respBytes = new byte[respBuffer.remaining()];
            respBuffer.get(respBytes);
            return new ByteArrayInputStream(respBytes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            nioConnectionPool.releaseConnection(peer,channel);
        }
    }


    @Override
    public void close() {

    }
}
