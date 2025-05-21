package com.connection;


import com.protocol.Peer;
import com.worker.BossServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class NIOConnectionPool {
    private final Map<Peer, LinkedBlockingQueue<SocketChannel>> pool = new ConcurrentHashMap<>();
    private static volatile NIOConnectionPool instance = null;
    private final int maxConnectionsPerAddress = 5;
    private NIOConnectionPool(){}
    public static NIOConnectionPool getNIOConnectionPool(){
        if(instance==null)
        {
            synchronized (NIOConnectionPool.class)
            {
                if(instance==null)
                {
                    instance = new NIOConnectionPool();
                }
            }
        }
        return instance;
    }
    /**
     * 初始化连接池
     * @param peer 服务端地址
     */
    public void initConnections(Peer peer) {
        if(pool.containsKey(peer)){
            return;
        }
        LinkedBlockingQueue<SocketChannel> connections = new LinkedBlockingQueue<>();
        for (int i = 0; i < maxConnectionsPerAddress; i++) {
            try {
                SocketChannel sc = SocketChannel.open();
                sc.connect(new InetSocketAddress(peer.getHost(), peer.getPort()));
                connections.offer(sc);
            } catch (IOException e) {
                log.error("Failed to connect to peer: {}", peer, e);
            }
        }
        pool.put(peer, connections);
    }

    /**
     * 获取一个连接
     */
    public SocketChannel getConnection(Peer peer) throws Exception {
        LinkedBlockingQueue<SocketChannel> connections = pool.get(peer);
        if (connections == null || connections.isEmpty()) {
            throw new RuntimeException("No available connections for peer: " + peer);
        }
        return connections.take(); // 阻塞获取
    }

    /**
     * 使用完后归还连接
     */
    public void releaseConnection(Peer peer, SocketChannel channel) {
        if (channel != null && channel.isConnected()) {
            pool.get(peer).offer(channel);
        }
    }

    /**
     * 关闭所有连接
     */
    public void closeAll() {
        for (Map.Entry<Peer, LinkedBlockingQueue<SocketChannel>> entry : pool.entrySet()) {
            for (SocketChannel sc : entry.getValue()) {
                try {
                    sc.close();
                } catch (IOException e) {
                    log.warn("Failed to close socket for peer: {}", entry.getKey(), e);
                }
            }
        }
        pool.clear();
    }
}
