package com.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 哈希负载均衡
 */
public class HashLoadBalancer implements LoadBalancer {
    private final String key;

    public HashLoadBalancer(String key) {
        this.key = key;
    }

    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        int hashCode = key.hashCode();
        int index = Math.abs(hashCode) % addresses.size();
        return addresses.get(index);
    }
} 