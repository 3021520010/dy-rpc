package com.nio;


import com.registry.RedisServiceRegistry;
import com.server.Server;
import com.server.ServerConfig;
import com.service.Test1;
import com.service.impl.Test1Impl;
import com.service.impl.TestImpl;
import com.service.Test;

public class NioServerDemo {
    public static void main(String[] args) throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        // 启动服务端
        Server server = new Server(serverConfig);
        server.setServiceRegistry(new RedisServiceRegistry("192.168.88.132",6379));
        //server.register(Test.class,new TestImpl());
        //server.register(Test1.class,new Test1Impl());
//        RequestHandler requestHandler=new NioRequestHandler();
        // server.init(3000, requestHandler);
        server.start();
        //new Thread(server::start).start();


        //server.stop();
    }
}
