package com.dy.dyrpcspringbootclient.tetsClient;


import com.annotation.RpcReference;
import com.service.Test;

import com.service.impl.TestImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testBean {
    //TODO 可以修改注解为类名，这样就支持一个接口多个实现类,或者name添加字段
    @RpcReference(interfaceClass = Test.class, retryCount = 2,retryTime = 500)
    private  Test test;


    @RequestMapping("/test")
   public void test(){
            System.out.println(test.add(1,2));
   }
}
