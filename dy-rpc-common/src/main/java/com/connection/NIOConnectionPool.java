package com.connection;

import com.protocol.Peer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NIOConnectionPool {
    private final Map<Peer, LinkedBlockingQueue<SocketChannel>> pool = new ConcurrentHashMap<>();
    private final Map<Peer, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();
    private static volatile NIOConnectionPool instance = null;

    // 最大连接数
    private final int maxConnectionsPerAddress = 5;

    private NIOConnectionPool(){}

    public static NIOConnectionPool getNIOConnectionPool(){
        if(instance == null) {
            synchronized (NIOConnectionPool.class) {
                if(instance == null) {
                    instance = new NIOConnectionPool();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化连接池
     */
    public void initConnections(Peer peer) {
        pool.putIfAbsent(peer, new LinkedBlockingQueue<>());
        connectionCounts.putIfAbsent(peer, new AtomicInteger(0));

        LinkedBlockingQueue<SocketChannel> connections = pool.get(peer);
        AtomicInteger count = connectionCounts.get(peer);

        // 创建初始连接数，直到 maxConnectionsPerAddress
        while (count.get() < maxConnectionsPerAddress) {
            SocketChannel sc = createConnection(peer);
            if (sc != null) {
                connections.offer(sc);
                count.incrementAndGet();
            }
        }
    }

    /**
     * 创建连接
     */
    private SocketChannel createConnection(Peer peer) {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(new InetSocketAddress(peer.getHost(), peer.getPort()));
            while (!sc.finishConnect()) {
                Thread.yield();
            }
            log.info("创建连接成功 {}",sc);
            return sc;
        } catch (IOException e) {
            log.error("建立连接失败 {}", peer, e);
            return null;
        }
    }

    public SocketChannel getConnection(Peer peer) throws Exception {
        pool.putIfAbsent(peer, new LinkedBlockingQueue<>());
        connectionCounts.putIfAbsent(peer, new AtomicInteger(0));

        LinkedBlockingQueue<SocketChannel> connections = pool.get(peer);
        AtomicInteger count = connectionCounts.get(peer);

        // 先尝试从池里获取连接，等待最多5秒
        SocketChannel channel = connections.poll(5, TimeUnit.SECONDS);
        if (channel != null && channel.isConnected() && channel.isOpen()) {
            return channel;
        }

        // 如果获取失败且连接数未达到最大，尝试创建新连接
        if (count.get() < maxConnectionsPerAddress) {
            synchronized (count) {
                if (count.get() < maxConnectionsPerAddress) {
                    SocketChannel newChannel = createConnection(peer);
                    if (newChannel != null) {
                        count.incrementAndGet();
                        return newChannel;
                    }
                }
            }
        }

        // 等待池中连接再次尝试获取（可选，避免立刻失败）
        channel = connections.poll(5, TimeUnit.SECONDS);
        if (channel != null && channel.isConnected() && channel.isOpen()) {
            return channel;
        }

        throw new RuntimeException("获取连接失败，没有可用的channel: " + peer);
    }

    public void releaseConnection(Peer peer, SocketChannel channel) {
        if (channel != null && channel.isConnected() && channel.isOpen()) {
            LinkedBlockingQueue<SocketChannel> connections = pool.get(peer);
            AtomicInteger count = connectionCounts.get(peer);
            if (connections != null && count != null) {
                // 限制连接池大小，超过则关闭连接释放资源
                if (connections.size() >= maxConnectionsPerAddress) {
                    try {
                        channel.close();
                        count.decrementAndGet();
                        log.info("连接池已满，关闭多余连接 {}", channel);
                    } catch (IOException e) {
                        log.warn("关闭连接异常", e);
                    }
                } else {
                    connections.offer(channel);
                }
            }
        } else {
            try {
                channel.close();
            } catch (IOException ignored) {}
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
                    log.error("关闭连接失败 {}", entry.getKey(), e);
                }
            }
        }
        pool.clear();
        connectionCounts.clear();
    }
}
