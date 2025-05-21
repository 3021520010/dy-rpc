package com.client;


import com.code.service.Decoder;
import com.code.service.Encoder;
import com.config.ClientConfig;
import com.connection.NIOConnectionPool;
import com.invoker.RemoteInvoker;
import com.loadbalance.LoadBalancer;
import com.protocol.Peer;
import com.registry.RedisServiceRegistry;
import com.service.ServiceRegistry;
import com.service.TransportClient;
import com.service.TransportSelector;
import com.utils.ReflectionUtils;
import lombok.Data;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端的配置
 */
@Data
public class Client {
    private ClientConfig config;
    private Encoder encoder;
    private Decoder decoder;
    //private TransportSelector selector;
    private NIOConnectionPool connectionPool;
    private ServiceRegistry serviceRegistry;
    private LoadBalancer loadBalancer;
    private ConcurrentHashMap<Class,Boolean> peers = new ConcurrentHashMap<>();

    public Client() {
        this(new ClientConfig());
    }

    public Client(ClientConfig config) {
        this.config = config;
        this.encoder = ReflectionUtils.newInstance(config.getEncoderClass());
        this.decoder = ReflectionUtils.newInstance(config.getDecoderClass());
        //this.selector = ReflectionUtils.newInstance(config.getSelectorClass());
        this.loadBalancer = ReflectionUtils.newInstance(config.getLoadBalancerClass());
        this.connectionPool = NIOConnectionPool.getNIOConnectionPool();
    }
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public <T> T getProxy(Class<T> clazz,int retryCount, int retryTime) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[]{clazz},
                new RemoteInvoker(clazz, encoder, decoder, serviceRegistry, loadBalancer,retryCount,retryTime));
    }

}
