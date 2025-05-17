package com.test;

import com.client.Client;
import com.config.ClientConfig;

import com.loadbalance.RoundRobinLoadBalancer;
import com.registry.RedisServiceRegistry;
import com.service.Test;
import com.service.Test1;

public class TestClient {
    public static void main(String[] args) throws InterruptedException {
        ClientConfig config = new ClientConfig();
        config.setLoadBalancerClass(RoundRobinLoadBalancer.class);
        Client client = new Client(config);
        client.setServiceRegistry(new RedisServiceRegistry("192.168.88.132", 6379));
         Test test= client.getProxy(Test.class);
        //Test1 test1= client.getProxy(Test1.class);
        System.out.println(test.add(1, 2));
        //System.out.println(test1.sub(10, 2));
    }
}
