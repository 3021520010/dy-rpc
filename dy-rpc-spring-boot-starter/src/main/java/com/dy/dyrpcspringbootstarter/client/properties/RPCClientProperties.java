package com.dy.dyrpcspringbootstarter.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "rpc")
@Data
public class RPCClientProperties {
    private LoadBalanceType loadbalance = LoadBalanceType.ROUND_ROBIN;
    private String redisHost = "192.168.88.132";
    private int redisPort = 6379;
    public enum LoadBalanceType {
        ROUND_ROBIN("RoundRobinLoadBalancer"),
        RANDOM("RandomLoadBalancer"),
        HASH("HashLoadBalancer");

        private final String className;

        LoadBalanceType(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }
}
