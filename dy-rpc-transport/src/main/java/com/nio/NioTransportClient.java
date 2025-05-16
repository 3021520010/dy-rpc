package com.nio;

import com.protocol.Peer;
import com.service.TransportClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NioTransportClient implements TransportClient {

   private Peer peer;
    public NioTransportClient() {

    }
    public void init(Peer peer){
        this.peer = peer;
    }
    @Override
    public InputStream write(InputStream data) {
        try {
            // 建立阻塞连接
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(peer.getHost(), peer.getPort()));
            sc.configureBlocking(true); // 设置为阻塞，简化逻辑

            // ========== 写请求 ==========
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            data.transferTo(byteBuffer);
            byte[] requestBytes = byteBuffer.toByteArray();

            // 写入长度（4字节） + 数据
            ByteBuffer buffer = ByteBuffer.allocate(4 + requestBytes.length);
            buffer.putInt(requestBytes.length);
            buffer.put(requestBytes);
            buffer.flip();
            while (buffer.hasRemaining()) {
                sc.write(buffer);
            }

            // ========== 读响应 ==========
            ByteBuffer lenBuf = ByteBuffer.allocate(4);
            while (lenBuf.hasRemaining()) {
                if (sc.read(lenBuf) == -1) {
                    throw new RuntimeException("服务器断开连接");
                }
            }
            lenBuf.flip();
            int respLen = lenBuf.getInt();

            ByteBuffer respBuf = ByteBuffer.allocate(respLen);
            while (respBuf.hasRemaining()) {
                if (sc.read(respBuf) == -1) {
                    throw new RuntimeException("服务器断开连接");
                }
            }
            respBuf.flip();
            byte[] respBytes = new byte[respBuf.remaining()];
            respBuf.get(respBytes);

            // 返回响应
            sc.close();
            return new ByteArrayInputStream(respBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {

    }
}
