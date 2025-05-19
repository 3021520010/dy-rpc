package com.dy.dyrpcspringbootserver.service.impl;


import com.service.Test1;

public class Test1Impl implements Test1 {
    @Override
    public int add(int a, int b) {
        return a+b;
    }

    @Override
    public int sub(int a, int b) {
        return a-b;
    }
}
