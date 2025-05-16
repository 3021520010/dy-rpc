package com.config;

import com.code.json.JSONDecoder;
import com.code.json.JSONEncoder;
import com.code.service.Decoder;
import com.code.service.Encoder;
import com.loadbalance.LoadBalancer;
import com.loadbalance.RoundRobinLoadBalancer;
import com.nio.NioTransportClient;
import com.registry.RedisServiceRegistry;
import com.selector.RandomTransportSelector;
import com.service.ServiceRegistry;
import com.service.TransportClient;
import com.service.TransportSelector;
import lombok.Data;

@Data
public class ClientConfig {
    private Class<? extends TransportClient> transportClientClass = NioTransportClient.class;
    private Class<? extends Encoder> encoderClass = JSONEncoder.class;
    private Class<? extends Decoder> decoderClass = JSONDecoder.class;
    private Class<? extends LoadBalancer> loadBalancerClass = RoundRobinLoadBalancer.class;
    private Class<? extends ServiceRegistry> serviceRegistryClass = RedisServiceRegistry.class;
    private Class<? extends TransportSelector> selectorClass = RandomTransportSelector.class;
    //private Class<? extends TransportSelector> selectorClass = RandomTransportSelector.class;
    //  每个peer的连接数
    private int connectCount = 1;
}
