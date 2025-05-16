package com.server;

import com.protocol.Request;
import com.protocol.ServiceDescriptor;

import com.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 管理rpc 服务，将他们注册到内存中，供客户端调用
 */
public class ServiceManager  {

    private static volatile ServiceManager instance;

    private ServiceManager() {
        services = new ConcurrentHashMap<>();
    }

    public static ServiceManager getInstance() {
        if (instance == null) {
            synchronized (ServiceManager.class) {
                if (instance == null) {
                    instance = new ServiceManager();
                }
            }
        }
        return instance;
    }
    /**
     * 存储服务实例，
     */
    private Map<ServiceDescriptor, ServiceInstance>services;
    public <T> void register(Class<T> interfaceClass, T bean)
    {
        Method[] methods = ReflectionUtils.getPublicMethods(interfaceClass);
        for (Method method : methods) {
            /*将一个接口的多个方法注册到服务管理器中，每个方法对应一个服务实例*/
            ServiceInstance serviceInstance = new ServiceInstance(bean,method);
            ServiceDescriptor serviceDescriptor = ServiceDescriptor.from(interfaceClass,method);
            services.put(serviceDescriptor,serviceInstance);
        }
    }
    public ServiceInstance lookup(Request request)
    {
        /*根据请求的service描述符，找到对应的服务实例*/
        ServiceDescriptor descriptor = request.getService();
        return services.get(descriptor);
        //return (ServiceInstance) services.get(descriptor).getTarget();
    }
}
