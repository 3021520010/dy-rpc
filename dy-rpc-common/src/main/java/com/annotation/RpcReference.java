package com.annotation;

import com.loadbalance.LoadBalancer;
import com.loadbalance.RoundRobinLoadBalancer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {

    /**
     * 指定该引用对应的接口类型（必须指定）
     */
    Class<?> interfaceClass();

    /**
     * 失败重试次数，默认值为 3
     */
    int retryCount() default 3;

    /**
     * 重试间隔时间（毫秒），默认值为 1000
     */
    int retryTime() default 1000;

    /**
     * 一致性哈希中用户指定的 key（如 userId），可选
     */
    String hashKey() default "";

    /**
     * 负载均衡策略，默认使用轮询
     */
    Class<? extends LoadBalancer> loadBalancer() default RoundRobinLoadBalancer.class;
}
