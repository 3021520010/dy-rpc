package com.dy.dyrpcspringbootstarter.factory;

import com.loadbalance.LoadBalancer;

import java.util.ServiceLoader;

public class LoadBalancerFactory {
    public static LoadBalancer getByName(String name) {
        ServiceLoader<LoadBalancer> loader = ServiceLoader.load(LoadBalancer.class);
        for (LoadBalancer lb : loader) {
            if (lb.getClass().getSimpleName().equalsIgnoreCase(name )) {
                return lb;
            }
        }
        throw new RuntimeException("No LoadBalancer found for name: " + name);
    }
}
