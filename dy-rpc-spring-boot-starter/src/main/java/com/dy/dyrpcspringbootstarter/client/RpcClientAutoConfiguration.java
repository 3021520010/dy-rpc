package com.dy.dyrpcspringbootstarter.client;

import com.client.Client;
import com.code.enums.TransportCodeType;
import com.config.ClientConfig;
import com.dy.dyrpcspringbootstarter.client.properties.RPCClientProperties;
import com.dy.dyrpcspringbootstarter.factory.LoadBalancerFactory;
import com.dy.dyrpcspringbootstarter.properties.RPCProperties;
import com.loadbalance.LoadBalancer;
import com.loadbalance.RoundRobinLoadBalancer;
import com.registry.RedisServiceRegistry;
import com.registry.ZookeeperServiceRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Client.class)
@ComponentScan(basePackages = "com.client")
@EnableConfigurationProperties(RPCProperties.class)
public class RpcClientAutoConfiguration {
    @Bean
    public Client rpcClient(RPCProperties rpcProperties) {
        RPCClientProperties properties=rpcProperties.getClient();
        ClientConfig config = new ClientConfig();
        TransportCodeType transportCodeType = rpcProperties.getTransportCodeType();
        config.setEncoderClass(transportCodeType.getEncoderInstance());
        config.setDecoderClass(transportCodeType.getDecoderInstance());
        Client client = new Client(config);
        if(properties.getLoadbalance()!=null){
            RPCClientProperties.LoadBalanceType loadbalance = properties.getLoadbalance();
            LoadBalancer lb = LoadBalancerFactory.getByName(loadbalance.getClassName());
            client.setLoadBalancer(lb);
        }
        if(rpcProperties.getRegistry().getType().equals("redis")){
            client.setServiceRegistry(new RedisServiceRegistry(rpcProperties.getRegistry().getRedis().getHost(), rpcProperties.getRegistry().getRedis().getPort()));
        }else{
            client.setServiceRegistry(new ZookeeperServiceRegistry(rpcProperties.getRegistry().getZookeeper().getHost(), rpcProperties.getRegistry().getZookeeper().getPort()));
        }
        return client;
    }
}
