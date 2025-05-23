package com.dy.dyrpcspringbootstarter.properties;

import com.code.enums.TransportCodeType;
import com.code.hessian.HessianDecoder;
import com.code.hessian.HessianEncoder;
import com.code.json.JSONDecoder;
import com.code.json.JSONEncoder;
import com.code.service.Decoder;
import com.code.service.Encoder;
import com.dy.dyrpcspringbootstarter.client.properties.RPCClientProperties;
import com.dy.dyrpcspringbootstarter.server.config.RPCServerProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "rpc")
public class RPCProperties {
    @NestedConfigurationProperty
    private RPCClientProperties client=new RPCClientProperties();

    @NestedConfigurationProperty
    private RPCServerProperties server=new RPCServerProperties();

    private Registry registry=new Registry();
    private Transport transport=new Transport();
    private TransportCodeType transportCodeType = TransportCodeType.JSON;




    @Data
    public static class Registry {
        private String type="redis";
        private Redis redis=new Redis();
        private Zookeeper zookeeper=new Zookeeper();
    }

    @Data
    public static class Redis {
        private String host="192.168.88.132";
        private int port=6379;
    }

    @Data
    public static class Zookeeper {
        private String host="192.168.88.132";
        private int port=2181;
    }

    @Data
    public static class Transport {
        private String type="nio";
        private Nio nio=new Nio();
        private Netty netty=new Netty();
    }

    @Data
    public static class Nio {
        private int port=3000;
    }

    @Data
    public static class Netty {
        private int bossThreads;
        private int port;
    }
}
