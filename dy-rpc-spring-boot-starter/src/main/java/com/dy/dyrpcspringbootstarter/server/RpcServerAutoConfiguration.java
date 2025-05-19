package com.dy.dyrpcspringbootstarter.server;


import com.registry.RedisServiceRegistry;
import com.server.Server;
import com.server.ServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Server.class)
@ComponentScan(basePackages = "com.server") // 替换为实际包名
public class RpcServerAutoConfiguration {

    @Bean
    public Server rpcServer() {
        ServerConfig config = new ServerConfig();
        Server server = new Server(config);
        server.setServiceRegistry(new RedisServiceRegistry("192.168.88.132", 6379));
        return server;
    }

}
