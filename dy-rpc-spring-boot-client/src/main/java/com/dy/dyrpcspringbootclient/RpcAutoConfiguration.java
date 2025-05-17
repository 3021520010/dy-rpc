package com.dy.dyrpcspringbootclient;

import com.client.Client;
import com.config.ClientConfig;
import com.loadbalance.RoundRobinLoadBalancer;
import com.registry.RedisServiceRegistry;
import com.service.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Client.class)
@ComponentScan(basePackages = "com.client")
public class RpcAutoConfiguration {
    @Bean
    public Client rpcServer() {
        ClientConfig config = new ClientConfig();
        config.setLoadBalancerClass(RoundRobinLoadBalancer.class);
        Client client = new Client(config);
        client.setServiceRegistry(new RedisServiceRegistry("192.168.88.132", 6379));
        return client;
    }
}
