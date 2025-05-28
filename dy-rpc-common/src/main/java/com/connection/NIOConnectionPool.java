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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NIOConnectionPool {

    private final Map<Peer, LinkedBlockingQueue<PooledConnection>> pool = new ConcurrentHashMap<>();
    private final Map<Peer, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();
    private final int maxConnectionsPerAddress = 20;
    private final int coreConnectionsPerAddress = 5;
    private static volatile NIOConnectionPool instance = null;
    private int workerCount = 2;
    private NioSelectorWorker[] workers;
    private int workerIndex = 0;
    private final Map<Peer, LinkedBlockingQueue<CompletableFuture<SocketChannel>>> waitingQueueMap = new ConcurrentHashMap<>();

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
    public CompletableFuture<SocketChannel> getConnectionAsync(Peer peer) {
        pool.putIfAbsent(peer, new LinkedBlockingQueue<>());
        connectionCounts.putIfAbsent(peer, new AtomicInteger(0));
        waitingQueueMap.putIfAbsent(peer, new LinkedBlockingQueue<>());

        LinkedBlockingQueue<PooledConnection> connections = pool.get(peer);
        AtomicInteger count = connectionCounts.get(peer);
        LinkedBlockingQueue<CompletableFuture<SocketChannel>> waitingQueue = waitingQueueMap.get(peer);

        // 尝试获取空闲连接
        for (PooledConnection conn : connections) {
            if (conn.isValid() && conn.tryUse()) {
                return CompletableFuture.completedFuture(conn.getChannel());
            }
        }

        // 可以创建新连接
        synchronized (count) {
            if (count.get() < maxConnectionsPerAddress) {
                SocketChannel sc = createConnection(peer);
                if (sc != null) {
                    PooledConnection newConn = new PooledConnection(sc);
                    newConn.tryUse();
                    count.incrementAndGet();
                    return CompletableFuture.completedFuture(sc);
                }
            }
        }

        // 没有空闲也不能创建，排队等待连接归还
        CompletableFuture<SocketChannel> future = new CompletableFuture<>();
        waitingQueue.offer(future);
        // 在 future 超时后移除
        future.orTimeout(300, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    waitingQueue.remove(future); // 防止已过期的 future 被处理
                    return null;
                });

        return future;
    }


//    public void releaseConnection(Peer peer, SocketChannel channel) {
//        if (channel == null || !channel.isConnected()) return;
//
//        LinkedBlockingQueue<PooledConnection> connections = pool.get(peer);
//        if (connections == null) return;
//
//        for (PooledConnection conn : connections) {
//            if (conn.getChannel() == channel) {
//                conn.release();
//                return;
//            }
//        }
//
//        // 是新建的连接，还未归还过，直接包裹后归还
//        PooledConnection conn = new PooledConnection(channel);
//        conn.release();
//        connections.offer(conn);
//    }
public void releaseConnection(Peer peer, SocketChannel channel) {
    if (channel == null || !channel.isConnected()) return;

    LinkedBlockingQueue<PooledConnection> connections = pool.get(peer);
    LinkedBlockingQueue<CompletableFuture<SocketChannel>> waitingQueue = waitingQueueMap.get(peer);
    if (connections == null) return;

    for (PooledConnection conn : connections) {
        if (conn.getChannel() == channel) {
            conn.release();

            // 看看有没有人正在等待连接
            CompletableFuture<SocketChannel> waiter = waitingQueue != null ? waitingQueue.poll() : null;
            if (waiter != null) {
                if (conn.tryUse()) {
                    waiter.complete(channel); // 分配给等待者
                    return;
                }
            }

            // 没有等待者，或者被别的线程抢走，就放回连接池
            connections.offer(conn);
            return;
        }
    }

    // 是新建的连接
    PooledConnection newConn = new PooledConnection(channel);
    newConn.release();

    CompletableFuture<SocketChannel> waiter = waitingQueue != null ? waitingQueue.poll() : null;
    if (waiter != null && newConn.tryUse()) {
        waiter.complete(channel);
    } else {
        connections.offer(newConn);
    }
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
