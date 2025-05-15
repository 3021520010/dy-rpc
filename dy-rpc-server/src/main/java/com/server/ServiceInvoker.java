package com.server;


import com.protocol.Request;
import com.utils.ReflectionUtils;

/**
 * 调用具体服务
 */
public class ServiceInvoker {
    public Object invoke(ServiceInstance serviceInstance, Request request)
    {
        return ReflectionUtils.invoke(serviceInstance.getTarget(),serviceInstance.getMethod(),request.getParameters());
    }
}
