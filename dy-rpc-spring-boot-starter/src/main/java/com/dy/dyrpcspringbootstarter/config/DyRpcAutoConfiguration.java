package com.dy.dyrpcspringbootstarter.config;

import com.dy.dyrpcspringbootstarter.client.RpcClientAutoConfiguration;
import com.dy.dyrpcspringbootstarter.client.processor.RpcClientProcessor;
import com.dy.dyrpcspringbootstarter.client.properties.RPCClientProperties;
import com.dy.dyrpcspringbootstarter.server.RpcServerAutoConfiguration;
import com.dy.dyrpcspringbootstarter.server.processor.RpcServerProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    RpcClientAutoConfiguration.class,
    RpcServerAutoConfiguration.class,
    RpcClientProcessor.class,
    //RPCClientProperties.class,
    RpcServerProcessor.class,
    //RPCServerProperties.class,
})
public class DyRpcAutoConfiguration {
}
