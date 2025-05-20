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
import java.util.concurrent.ArrayBlockingQueue;

public class NioTransportClient implements TransportClient {

   private Peer peer;
   private NIOConnectionPool nioConnectionPool = NIOConnectionPool.getNIOConnectionPool();
    private NioSelectorWorker worker;
    private SocketChannel channel;
    public NioTransportClient() {

    }
    public void init(Peer peer){
        this.peer = peer;
        this.worker = new NioSelectorWorker("test"); // 每个客户端单独一个 worker 线程
        new Thread(worker).start();
        try {
            this.channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(peer.getHost(), peer.getPort()));
            // 注册连接任务
            worker.register(channel, SelectionKey.OP_CONNECT, () -> {
                try {
                    if (channel.finishConnect()) {
                        System.out.println("连接成功：" + peer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public InputStream write(InputStream data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            data.transferTo(bos);
            byte[] body = bos.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + body.length);
            buffer.putInt(body.length);
            buffer.put(body);
            buffer.flip();

            // 用阻塞队列同步等待响应
            ArrayBlockingQueue<byte[]> respQueue = new ArrayBlockingQueue<>(1);

            // 注册写任务
            worker.register(channel, SelectionKey.OP_WRITE, new Runnable() {
                boolean wrote = false;
                ByteBuffer writeBuf = buffer;
                ByteBuffer lenBuf = ByteBuffer.allocate(4);
                ByteBuffer readBuf = null;

                @Override
                public void run() {
                    try {
                        if (!wrote) {
                            channel.write(writeBuf);
                            if (!writeBuf.hasRemaining()) {
                                wrote = true;
                                // 切换为读
                                channel.register(worker.getSelector(), SelectionKey.OP_READ, this);
                            }
                        } else {
                            // 开始读取响应：先读长度
                            if (readBuf == null) {
                                channel.read(lenBuf);
                                if (!lenBuf.hasRemaining()) {
                                    lenBuf.flip();
                                    int len = lenBuf.getInt();
                                    readBuf = ByteBuffer.allocate(len);
                                }
                            } else {
                                channel.read(readBuf);
                                if (!readBuf.hasRemaining()) {
                                    respQueue.put(readBuf.array());
                                    // 清除 selectionKey
                                    channel.register(worker.getSelector(), 0);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // 同步等待结果
            byte[] resp = respQueue.take();
            return new ByteArrayInputStream(resp);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() {

    }
}
