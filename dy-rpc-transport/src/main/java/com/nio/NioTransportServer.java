package com.nio;

import com.service.RequestHandler;
import com.service.TransportServer;
import com.worker.BossServer;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioTransportServer implements TransportServer {
    private int port;
    private RequestHandler handler;
    //private BossServer bossServer;
    @Override
    public void init(int port, RequestHandler requestHandler) {
        this.handler = requestHandler;
        this.port = port;
//        BossServer bossServer = new BossServer(port, 3);
//        new Thread(bossServer).start();
    }

    @Override
    public void start() {
        new Thread(() -> {
            try {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(port));
                serverSocketChannel.configureBlocking(false);

                Selector selector = Selector.open();
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                while (true) {
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()){
                            ServerSocketChannel serverSocketChannel1 = (ServerSocketChannel) key.channel();
                            SocketChannel accept = serverSocketChannel1.accept();
                            accept.configureBlocking(false);
                            accept.register(selector, SelectionKey.OP_READ);
                        }else if (key.isReadable()){
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            try {
                                // 1. 读取长度
                                ByteBuffer lenBuf = ByteBuffer.allocate(4);
                                int read = clientChannel.read(lenBuf);
                                if (read == -1) {
                                    key.cancel();
                                    clientChannel.close();
                                    continue;
                                }
                                while (lenBuf.hasRemaining()) {
                                    if (clientChannel.read(lenBuf) == -1) {
                                        key.cancel();
                                        clientChannel.close();
                                        break;
                                    }
                                }
                                lenBuf.flip();
                                int dataLen = lenBuf.getInt();

                                // 2. 读取数据
                                ByteBuffer dataBuf = ByteBuffer.allocate(dataLen);
                                while (dataBuf.hasRemaining()) {
                                    if (clientChannel.read(dataBuf) == -1) {
                                        key.cancel();
                                        clientChannel.close();
                                        break;
                                    }
                                }
                                dataBuf.flip();
                                byte[] requestData = new byte[dataBuf.remaining()];
                                dataBuf.get(requestData);
                                //handler.onRequest(new ByteArrayInputStream(respBytes), clientChannel.socket().getOutputStream());
                                handler.onRequest(new ByteArrayInputStream(requestData), clientChannel);
                            }catch (Exception e){
                                key.cancel();
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        System.out.println("server启动成功监听端口" + port);
    }

    @Override
    public void stop() {

    }
}
