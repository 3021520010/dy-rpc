package com.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡接口
 */
public interface LoadBalancer {
    /**
     * 选择一个服务地址
     * @param addresses 服务地址列表
     * @return 选择的服务地址
     */
    InetSocketAddress select(List<InetSocketAddress> addresses,String hashKey);
} 