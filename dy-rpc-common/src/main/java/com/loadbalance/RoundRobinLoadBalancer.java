package com.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses,String key) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        int currentIndex = index.getAndIncrement();
        if (currentIndex >= addresses.size()) {
            index.set(0);
            currentIndex = 0;
        }
        return addresses.get(currentIndex);
    }
} 