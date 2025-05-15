package com.nio;


import com.server.Server;
import com.server.ServerConfig;
import com.service.RequestHandler;
import com.service.TransportServer;
import com.service.impl.testImpl;
import com.service.test;

public class NioServerDemo {
    public static void main(String[] args) throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        // 启动服务端
        Server server = new Server(serverConfig);
        server.register(test.class,new testImpl());
//        RequestHandler requestHandler=new NioRequestHandler();
        // server.init(3000, requestHandler);
        server.start();
        //new Thread(server::start).start();


        //server.stop();
    }
}
