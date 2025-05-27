package com.connection;

import com.protocol.Peer;
import com.worker.NioSelectorWorker;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.zookeeper.server.WorkerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NIOConnectionPool {

    private final Map<Peer, LinkedBlockingQueue<PooledConnection>> pool = new ConcurrentHashMap<>();
    private final Map<Peer, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();
    private final int maxConnectionsPerAddress = 10;
    private final int coreConnectionsPerAddress = 2;
    private static volatile NIOConnectionPool instance = null;
    private int workerCount = 1;
    private NioSelectorWorker[] workers;
    private int workerIndex = 0;

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
    private NIOConnectionPool() {
        workers = new NioSelectorWorker[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new NioSelectorWorker("客户端worker-" + i, null);
            Thread workerThread = new Thread(workers[i]);
            workerThread.start();
        }
    }

    public void initConnections(Peer peer) {
        pool.putIfAbsent(peer, new LinkedBlockingQueue<>());
        connectionCounts.putIfAbsent(peer, new AtomicInteger(0));

        var connections = pool.get(peer);
        var count = connectionCounts.get(peer);

        while (count.get() < coreConnectionsPerAddress) {
            SocketChannel sc = createConnection(peer);
            if (sc != null) {
                connections.offer(new PooledConnection(sc));
                count.incrementAndGet();
            }
        }
    }

    public SocketChannel createConnection(Peer peer) {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(new InetSocketAddress(peer.getHost(), peer.getPort()));
            while (!sc.finishConnect()) {
                Thread.yield();
            }
            log.info("创建连接成功 {}", sc);
            return sc;
        } catch (IOException e) {
            log.error("建立连接失败 {}", peer, e);
            return null;
        }
    }
    public NioSelectorWorker getWorker(){
        return workers[workerIndex++ % workers.length];
    }

    public SocketChannel getConnection(Peer peer) throws Exception {
        pool.putIfAbsent(peer, new LinkedBlockingQueue<>());
        connectionCounts.putIfAbsent(peer, new AtomicInteger(0));

        LinkedBlockingQueue<PooledConnection> connections = pool.get(peer);
        AtomicInteger count = connectionCounts.get(peer);

        PooledConnection usableConn = null;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);

        while (System.nanoTime() < deadline) {
            PooledConnection conn = connections.poll(100, TimeUnit.MILLISECONDS);
            if (conn != null && conn.isValid()) {
                if (conn.tryUse()) {
                    usableConn = conn;
                    break;
                } else {
                    // 正在使用中，放回池子尾部
                    connections.offer(conn);
                }
            }
        }

        if (usableConn != null) {
            SocketChannel channel = usableConn.getChannel();
            return channel;
        }

        // 还能创建新连接
        if (count.get() < maxConnectionsPerAddress) {
            synchronized (count) {
                if (count.get() < maxConnectionsPerAddress) {
                    SocketChannel sc = createConnection(peer);
                    if (sc != null) {
                        PooledConnection newConn = new PooledConnection(sc);
                        newConn.tryUse(); // 标记为使用中
                        count.incrementAndGet();
                        return newConn.getChannel();
                    }
                }
            }
        }
        return null;
        //throw new RuntimeException("获取连接失败: 无可用连接，且连接数已达上限");
    }

    public void releaseConnection(Peer peer, SocketChannel channel) {
        if (channel == null || !channel.isConnected()) return;

        LinkedBlockingQueue<PooledConnection> connections = pool.get(peer);
        if (connections == null) return;

        for (PooledConnection conn : connections) {
            if (conn.getChannel() == channel) {
                conn.release();
                return;
            }
        }

        // 是新建的连接，还未归还过，直接包裹后归还
        PooledConnection conn = new PooledConnection(channel);
        conn.release();
        connections.offer(conn);
    }

    public void closeAll() {
        for (Map.Entry<Peer, LinkedBlockingQueue<PooledConnection>> entry : pool.entrySet()) {
            for (PooledConnection conn : entry.getValue()) {
                try {
                    conn.getChannel().close();
                } catch (IOException e) {
                    log.error("关闭连接失败 {}", entry.getKey(), e);
                }
            }
        }
        pool.clear();
        connectionCounts.clear();
    }
}
class PooledConnection {
    private final SocketChannel channel;
    private final AtomicBoolean inUse = new AtomicBoolean(false);

    public PooledConnection(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public boolean tryUse() {
        return inUse.compareAndSet(false, true);
    }

    public void release() {
        inUse.set(false);
    }

    public boolean isValid() {
        return channel != null && channel.isConnected();
    }
}
