package com.dy.dyrpcspringbootclient.service.impl;

import com.annotation.RpcService;
import com.service.Test1;

@RpcService(interfaceClass = Test1.class)
public class Test1Impl implements Test1 {
    @Override
    public int add(int a, int b) {
        return 0;
    }

    @Override
    public int sub(int a, int b) {
        return 0;
    }
}
