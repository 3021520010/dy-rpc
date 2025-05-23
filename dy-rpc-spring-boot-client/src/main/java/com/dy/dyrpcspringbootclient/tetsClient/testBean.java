package com.dy.dyrpcspringbootclient.tetsClient;


import com.annotation.RpcReference;
import com.service.Test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testBean {
    @RpcReference(interfaceClass = Test.class, retryCount = 2,retryTime = 500)
    private  Test test;


    @RequestMapping("/test")
   public void test(){
            System.out.println(test.add(1,2));
   }
}
