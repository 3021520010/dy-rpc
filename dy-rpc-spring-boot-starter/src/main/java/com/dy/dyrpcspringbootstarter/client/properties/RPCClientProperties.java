package com.dy.dyrpcspringbootstarter.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;



@Data
public class RPCClientProperties {
    private LoadBalanceType loadbalance = LoadBalanceType.ROUND_ROBIN;
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
