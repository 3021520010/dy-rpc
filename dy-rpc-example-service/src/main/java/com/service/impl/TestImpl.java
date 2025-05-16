package com.service.impl;

import com.annotation.RpcService;
import com.service.Test;
@RpcService(interfaceClass = Test.class)
public class TestImpl implements Test {
    @Override
    public int add(int a, int b) {
        return a+b;
    }
}
