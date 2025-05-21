package com.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

public class RandomLoadBalancer implements LoadBalancer{
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String hashKey) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        int index= (int) (Math.random() * addresses.size());
        return addresses.get(index);
    }
}
