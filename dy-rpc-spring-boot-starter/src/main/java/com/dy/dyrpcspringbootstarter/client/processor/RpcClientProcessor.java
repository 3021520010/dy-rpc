package com.dy.dyrpcspringbootstarter.client.processor;

import com.annotation.RpcReference;
import com.client.Client;
import com.loadbalance.LoadBalancer;
import com.loadbalance.RoundRobinLoadBalancer;
import com.utils.ReflectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.UUID;

@Component
public class RpcClientProcessor implements BeanPostProcessor {

    @Autowired
    private Client client;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(RpcReference.class)) {
                RpcReference annotation = field.getAnnotation(RpcReference.class);
                Class<?> interfaceClass = annotation.interfaceClass();
                int retryCount = annotation.retryCount();
                int retryTime = annotation.retryTime();
                String key = annotation.hashKey();
                if (!"".equals(key)) {
                    key=UUID.randomUUID().toString();
                }
                Class<? extends LoadBalancer> loadBalancerClass = annotation.loadBalancer();
                LoadBalancer loadBalancer= ReflectionUtils.newInstance(loadBalancerClass);
                Object proxy= client.getProxy(interfaceClass,retryCount,retryTime,loadBalancer,key);
                field.setAccessible(true);
                try {
                    field.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bean;
    }
}
