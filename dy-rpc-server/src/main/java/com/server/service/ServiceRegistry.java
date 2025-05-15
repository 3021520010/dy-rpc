package com.server.service;

import com.protocol.Request;
import com.protocol.ServiceDescriptor;
import com.server.ServiceInstance;
import com.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务注册中心接口
 */
public interface ServiceRegistry {
    /**
     * 注册服务
     * @param serviceName 服务名称
     * @param address 服务地址
     */
    void register(String serviceName, InetSocketAddress address);

    /**
     * 注销服务
     * @param serviceName 服务名称
     * @param address 服务地址
     */
    void unregister(String serviceName, InetSocketAddress address);

    /**
     * 查找服务
     * @param serviceName 服务名称
     * @return 服务地址列表
     */
    List<InetSocketAddress> lookup(String serviceName);
}