package com.dy.dyrpcspringbootstarter.client.processor;

import com.annotation.RpcReference;
import com.client.Client;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

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
                Object proxy = client.getProxy(interfaceClass,retryCount,retryTime);
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
