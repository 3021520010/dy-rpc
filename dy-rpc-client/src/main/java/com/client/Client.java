package com.client;


import com.code.service.Decoder;
import com.code.service.Encoder;
import com.config.ClientConfig;
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
    private TransportSelector selector;
    private ServiceRegistry serviceRegistry;
    private LoadBalancer loadBalancer;
    private ConcurrentHashMap<InetSocketAddress,Boolean> peers = new ConcurrentHashMap<>();

    public Client() {
        this(new ClientConfig());
    }

    public Client(ClientConfig config) {
        this.config = config;
        this.encoder = ReflectionUtils.newInstance(config.getEncoderClass());
        this.decoder = ReflectionUtils.newInstance(config.getDecoderClass());
        this.selector = ReflectionUtils.newInstance(config.getSelectorClass());
        this.loadBalancer = ReflectionUtils.newInstance(config.getLoadBalancerClass());
    }
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public <T> T getProxy(Class<T> clazz) {
        List<InetSocketAddress> addresses = serviceRegistry.lookup(clazz.getName());
        if (!addresses.isEmpty()) {
            // 使用负载均衡策略从多个地址中选择一个服务提供者
            InetSocketAddress address = loadBalancer.select(addresses);
            if (address != null) {
                // 初始化连接池，从选中的服务提供者地址创建多个连接实例，多个socket
                if(!peers.containsKey(address)){
                    selector.init(Arrays.asList(new Peer(address.getHostString(), address.getPort())),
                            config.getConnectCount(),
                            config.getTransportClientClass());
                    peers.put(address,true);
                }
            }
        }
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[]{clazz},
                new RemoteInvoker(clazz, encoder, decoder, selector));
    }

}
