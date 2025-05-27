package com.dy.dyrpcspringbootserver.service.impl;

import com.annotation.RpcService;

import com.service.Test;
import org.springframework.stereotype.Service;

@RpcService(interfaceClass = Test.class)
@Service
public class TestImpl implements Test {
    @Override
    public int add(int a, int b) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return a+b;
    }
}
