package com.dy.dyrpcspringbootstarter.processor;

import com.annotation.RpcService;
import com.server.Server;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RpcServiceProcessor implements ApplicationContextAware, InitializingBean {

    @Autowired
    private Server rpcServer;
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 启动后扫描所有 @RpcService 注解的 Bean
        Map<String, Object> beans = context.getBeansWithAnnotation(RpcService.class);
        for (Object serviceBean : beans.values()) {
            Class<?> implClass = serviceBean.getClass();
            RpcService annotation = implClass.getAnnotation(RpcService.class);
            Class<?> interfaceClass = annotation.interfaceClass();
            rpcServer.register((Class) interfaceClass, serviceBean);
            System.out.println("Registered RPC service: " + interfaceClass.getName());
        }
        rpcServer.start();
    }
}
