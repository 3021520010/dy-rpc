package com.loadbalance;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * 哈希负载均衡
 */
public class HashLoadBalancer implements LoadBalancer {
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String hashKey) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        int currentIndex = hashKey.hashCode() % addresses.size();
        return addresses.get(currentIndex);
    }
}

