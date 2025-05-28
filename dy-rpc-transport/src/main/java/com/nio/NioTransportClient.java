package com.nio;

import com.connection.NIOConnectionPool;
import com.protocol.Peer;
import com.registry.NioResponseRegistry;
import com.registry.RpcFuture;
import com.service.TransportClient;
import com.worker.BossServer;
import com.worker.ClientContext;
import com.worker.NioSelectorWorker;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

@Slf4j
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
    // 阻塞式调用
//    @Override
//    public InputStream write(InputStream data) {
//        try {
//            channel=nioConnectionPool.getConnection(peer);
//            if(channel==null){
//                throw new RuntimeException("获取channel失败，目前没有可用的channel");
//            }
//            // 1. 先将输入流内容读取为byte[]
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            data.transferTo(bos);
//            byte[] body = bos.toByteArray();
//            if(body.length>1024*1024){
//                throw new RuntimeException("client发送数据过大");
//            }
//            // 2. 构造写入缓冲区（4字节长度+数据）
//            ByteBuffer writeBuffer = ByteBuffer.allocate(4 + body.length);
//            writeBuffer.putInt(body.length);
//            writeBuffer.put(body);
//            writeBuffer.flip();
//
//            clientWrite(channel, writeBuffer);
//
//            // 4. 读取响应长度（4字节）
//            ByteBuffer lenBuffer = ByteBuffer.allocate(4);
//            while (lenBuffer.hasRemaining()) {
//                int read = channel.read(lenBuffer);
//                if (read == -1) {
//                    throw new IOException("服务器关闭连接");
//                }
//                if (read == 0) {
//                    Thread.sleep(10);
//                }
//            }
//            lenBuffer.flip();
//            int respLen = lenBuffer.getInt();
//            if(respLen>1024*1024){
//                throw new RuntimeException("client接收数据过大");
//            }
//            // 5. 读取响应数据
//            ByteBuffer respBuffer = ByteBuffer.allocate(respLen);
//            while (respBuffer.hasRemaining()) {
//                int read = channel.read(respBuffer);
//                if (read == -1) {
//                    throw new IOException("服务器关闭连接");
//                }
//                if (read == 0) {
//                    Thread.sleep(10);
//                }
//            }
//            respBuffer.flip();
//
//            // 6. 返回响应数据的InputStream
//            byte[] respBytes = new byte[respBuffer.remaining()];
//            respBuffer.get(respBytes);
//            return new ByteArrayInputStream(respBytes);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }finally {
//            if (channel != null && channel.isConnected() && !channel.socket().isClosed()) {
//                log.error("开始释放连接: {}", channel);
//                nioConnectionPool.releaseConnection(peer, channel);
//            }
//        }
//    }

    @Override
    public InputStream write(InputStream data) {
        CompletableFuture<InputStream> inputStreamCompletableFuture = sendAsync(data);
        try {
            return inputStreamCompletableFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }finally {
            if (channel != null && channel.isConnected() && !channel.socket().isClosed()) {
                log.error("开始释放连接: {}", channel);
                nioConnectionPool.releaseConnection(peer, channel);
            }
        }
        return null;
    }


    public CompletableFuture<InputStream> sendAsync(InputStream data)  {
       try {
           //channel=nioConnectionPool.getConnection(peer);
            channel=nioConnectionPool.getConnectionAsync(peer).get();
            if(channel==null){
                throw new RuntimeException("获取channel失败，目前没有可用的channel");
            }
           channel.configureBlocking(false);
           // 1. 构造要发送的数据
           byte[] body = data.readAllBytes();
           ByteBuffer buffer = ByteBuffer.allocate(4 + body.length);
           buffer.putInt(body.length);
           buffer.put(body);
           buffer.flip();

           // 2. 从连接池中获取 channel 和 selectorWorker
           NioSelectorWorker worker = nioConnectionPool.getWorker();

           // 3. 创建异步 future 和 context
           CompletableFuture<InputStream> future = new CompletableFuture<>();
           ClientContext context = new ClientContext(future);


           // 4. 注册读回调：异步接收响应
           worker.register(channel, SelectionKey.OP_READ, ch -> worker.clientRead(ch, context));
           // 5. 写数据（触发 OP_WRITE）
           worker.writeData(channel, buffer);


           // 6. 返回 future（调用方通过 thenApply 获取结果）
           return future;
       }catch (Exception e){
           e.printStackTrace();
       }
       return null;
    }

//    /**
//     * 测试半包现象
//     * @param data
//     * @return
//     */
//    public CompletableFuture<InputStream> sendAsync(InputStream data)  {
//    try {
//        channel=nioConnectionPool.getConnection(peer);
//        // channel=nioConnectionPool.createConnection(peer);
//        channel.configureBlocking(false);
//        // 1. 构造要发送的数据
//        byte[] body = data.readAllBytes();
//        ByteBuffer fullBuffer = ByteBuffer.allocate(4 + body.length);
//        fullBuffer.putInt(body.length);
//        fullBuffer.put(body);
//        fullBuffer.flip();
//// 取出完整字节数组
//        byte[] fullBytes = new byte[fullBuffer.remaining()];
//        fullBuffer.get(fullBytes);
//        // 模拟“半包发送”：只写部分数据（前 3 字节 + 剩余部分）
//        ByteBuffer part1 = ByteBuffer.wrap(fullBytes, 0, 3); // 只写前 3 个字节
//        ByteBuffer part2 = ByteBuffer.wrap(fullBytes, 3, fullBytes.length - 3); // 剩余部分
//        // 2. 从连接池中获取 channel 和 selectorWorker
//        NioSelectorWorker worker = nioConnectionPool.getWorker();
//
//        // 3. 创建异步 future 和 context
//        CompletableFuture<InputStream> future = new CompletableFuture<>();
//        ClientContext context = new ClientContext(future);
//
//
//        // 4. 注册读回调：异步接收响应
//        worker.register(channel, SelectionKey.OP_READ, (NioSelectorWorker.ReadHandler) ch -> worker.clientRead(ch, context));
//        worker.writeData(channel, part1);
//
//// 模拟网络延迟或不完整写入
//        Thread.sleep(1000);
//
//// 再发送剩下的部分
//        worker.writeData(channel, part2);
//
//
//        // 6. 返回 future（调用方通过 thenApply 获取结果）
//        return future;
//    }catch (Exception e){
//        e.printStackTrace();
//    }
//    return null;
//}



    @Override
    public void close() {

    }




}
