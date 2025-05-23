package com.dy.dyrpcspringbootstarter.server;


import com.dy.dyrpcspringbootstarter.properties.RPCProperties;
import com.dy.dyrpcspringbootstarter.server.config.RPCServerProperties;
import com.registry.RedisServiceRegistry;
import com.registry.ZookeeperServiceRegistry;
import com.server.Server;
import com.server.ServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Server.class)
@ComponentScan(basePackages = "com.server")
@EnableConfigurationProperties(RPCProperties.class)
public class RpcServerAutoConfiguration {

    @Bean
    public Server rpcServer(RPCProperties rpcProperties) {
        RPCServerProperties properties=rpcProperties.getServer();
        ServerConfig config = new ServerConfig();
        config.setPort(rpcProperties.getTransport().getNio().getPort());
        Server server = new Server(config);
        if(rpcProperties.getRegistry().getType().equals("redis")){
            server.setServiceRegistry(new RedisServiceRegistry(rpcProperties.getRegistry().getRedis().getHost(), rpcProperties.getRegistry().getRedis().getPort()));
        }else{
            server.setServiceRegistry(new ZookeeperServiceRegistry(rpcProperties.getRegistry().getZookeeper().getHost(), rpcProperties.getRegistry().getZookeeper().getPort()));
        }
        return server;
    }

}
