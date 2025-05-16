package com.server;

import com.code.service.Decoder;
import com.code.service.Encoder;
import com.protocol.Request;
import com.protocol.Response;
import com.service.ServiceRegistry;
import com.service.RequestHandler;
import com.service.TransportServer;
import com.utils.ReflectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class Server {
    private ServerConfig config;
    private TransportServer net;
    private ServiceManager serviceManager;
    //注册中心
    private ServiceRegistry serviceRegistry;
    private Encoder encoder;
    private Decoder decoder;
    private RequestHandler handler;
    private ServiceInvoker serviceInvoker;
    public Server(ServerConfig config) {
        this.config = config;
        this.net = ReflectionUtils.newInstance(this.config.getTransportClass());
        this.encoder = ReflectionUtils.newInstance(this.config.getEncoderClass());
        this.decoder = ReflectionUtils.newInstance(this.config.getDecoderClass());
        this.handler=requestHandler;
        serviceInvoker=new ServiceInvoker();
        serviceManager=ServiceManager.getInstance();
        this.net.init(this.config.getPort(), this.handler);
    }
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    public void start() {
        this.net.start();
    }
    public <T> void register(Class<T> interfaceClass, T bean) {
        serviceManager.register(interfaceClass, bean);
        if(serviceRegistry!=null){
            // 地址存服务器地址
            serviceRegistry.register(interfaceClass.getName(), new InetSocketAddress(config.getHost(), config.getPort()));
        }
    }
    public void stop() {
        this.net.stop();
    }
    private RequestHandler requestHandler=new RequestHandler() {
        @Override
        public void onRequest(InputStream receive, OutputStream toResp) {
            Response response = new Response();
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] temp = new byte[8192];
                int len;
                while ((len = receive.read(temp)) != -1) {
                    buffer.write(temp, 0, len);
                }
                byte[] inBytes = buffer.toByteArray();
                Request request = decoder.decode(inBytes, Request.class);
                ServiceInstance serviceInstance = serviceManager.lookup(request);
                Object ret = serviceInvoker.invoke(serviceInstance, request);
                response.setData(ret);
            } catch (Exception e) {
                response.setCode(1);
                response.setMessage("RpcServer get Error:" + e.getClass().getName() + " " + e.getMessage());
            } finally {
                byte[] outBytes = encoder.encode(response);
                try {
                    toResp.write(outBytes);
                } catch (IOException e) {
                }
            }
        }

        @Override
        public void onRequest(InputStream receive, SocketChannel channel) {
            Response response = new Response();
            try {
                Request request=decoder.decode(receive.readAllBytes(), Request.class);
                ServiceInstance serviceInstance = serviceManager.lookup(request);
                Object ret = serviceInvoker.invoke(serviceInstance, request);
                response.setData(ret);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try{
                    byte[] responseData = encoder.encode(response);
                    int len=responseData.length;
                    ByteBuffer buffer = ByteBuffer.allocate(4 + len);
                    buffer.putInt(len);
                    buffer.put(responseData);
                    buffer.flip(); // 切换为读模式
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                }catch (Exception e){

                }
            }
        }
    };

}

