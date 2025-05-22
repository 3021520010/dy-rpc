package com.registry;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NioResponseRegistry {
    private static final Map<SocketChannel, RpcFuture> futureMap = new ConcurrentHashMap<>();

    public static void put(SocketChannel channel, RpcFuture future) {
        futureMap.put(channel, future);
    }

    public static RpcFuture get(SocketChannel channel) {
        return futureMap.get(channel);
    }

    public static void remove(SocketChannel channel) {
        futureMap.remove(channel);
    }
}
