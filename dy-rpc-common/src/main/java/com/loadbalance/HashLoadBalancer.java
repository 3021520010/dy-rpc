//package com.loadbalance;
//
//import java.net.InetSocketAddress;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * 哈希负载均衡
// */
//public class HashLoadBalancer implements LoadBalancer {
//    @Override
//    public InetSocketAddress select(List<InetSocketAddress> addresses) {
//        if (addresses == null || addresses.isEmpty()) return null;
//
//        String key = request
//        int hash = key.hashCode();
//        return addresses.get(Math.abs(hash) % addresses.size());
//    }
//
//}
//
