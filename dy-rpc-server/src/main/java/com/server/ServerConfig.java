package com.server;

import com.code.json.JSONDecoder;
import com.code.json.JSONEncoder;
import com.code.service.Decoder;
import com.code.service.Encoder;
import com.nio.NioTransportServer;

import com.registry.RedisServiceRegistry;
import com.service.ServiceRegistry;
import com.service.TransportServer;
import lombok.Data;


@Data
public class ServerConfig {
    private Class<? extends TransportServer> transportClass= NioTransportServer.class;
    private Class<? extends Encoder> encoderClass= JSONEncoder.class;
    private Class<? extends Decoder> decoderClass= JSONDecoder.class;
    private Class<? extends ServiceRegistry> ServiceRegistryClass= RedisServiceRegistry.class;
    private String[] packages= new String[]{"com.service.impl"};
    private int port= 3000;
    private String host= "127.0.0.1";

}
