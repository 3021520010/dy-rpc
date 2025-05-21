package com.invoker;


import com.code.service.Decoder;
import com.code.service.Encoder;
import com.connection.NIOConnectionPool;
import com.loadbalance.LoadBalancer;
import com.nio.NioTransportClient;
import com.protocol.Peer;
import com.protocol.Request;
import com.protocol.Response;
import com.protocol.ServiceDescriptor;
import com.service.ServiceRegistry;
import com.service.TransportClient;
import com.service.TransportSelector;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class RemoteInvoker implements InvocationHandler {
    private Class clazzz;
    private Encoder encoder;
    private Decoder decoder;
    private ServiceRegistry registry;
    private LoadBalancer loadBalancer;
    public RemoteInvoker(Class clazzz,Encoder encoder,Decoder decoder,ServiceRegistry serviceRegistry,LoadBalancer loadBalancer){
        this.decoder=decoder;
        this.encoder=encoder;
        this.loadBalancer=loadBalancer;
        this.registry=serviceRegistry;
        this.clazzz=clazzz;
    }
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
        Request request=new Request();
        request.setService(ServiceDescriptor.from(clazzz,method));
        request.setParameters(args);
        Response response=invokeRemote(request);
        if(response==null||response.getCode()!=0){
            throw new IllegalStateException("发送请求失败"+response);
        }
        return response.getData();
    }

    private Response invokeRemote(Request request) {
        Response response = null;

        //TODO 可以修改次数
        int retryCount = 3;

        for (int i = 0; i < retryCount; i++) {
            TransportClient client = null;
            try {
                // 每次重试都重新选择一个连接（可能是另一个服务端）
                //获取活跃的连接
                List<InetSocketAddress> activeAddress = registry.lookup(clazzz.getName());

                if (activeAddress == null || activeAddress.isEmpty()) {
                    System.err.println("没有可用的服务端地址");
                    return null;
                }
                //负载均衡选择一个
                InetSocketAddress address = loadBalancer.select(activeAddress);
                client = new NioTransportClient();
                client.init(new Peer(address.getHostString(), address.getPort()));
                // 编码请求
                byte[] requestBytes = encoder.encode(request);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(bos);
                dos.write(requestBytes);
                dos.flush();

                InputStream in = new ByteArrayInputStream(bos.toByteArray());

                // 调用远程服务
                InputStream res = client.write(in);
                byte[] respBytes = res.readAllBytes();
                response = decoder.decode(respBytes, Response.class);

                System.out.println("客户端接收到信息: " + response);

                // 如果响应成功，则返回
                if (response != null && response.getCode() == 0) {
                    return response;
                } else {
                    System.err.println("调用失败，第 " + (i + 1) + " 次，服务端返回错误: " + response);
                }

            } catch (Exception e) {
                System.err.println("调用失败，第 " + (i + 1) + " 次，异常: " + e.getMessage());
                response = new Response();
                response.setCode(1);
                response.setMessage("RPC 调用异常: " + e.getMessage());

            } finally {
                // 不论成功失败，都释放连接
                if (client != null) {
                    client.close();
                }
            }
            // 失败后等待 1 秒
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        // 多次重试仍然失败
        return response;
    }
}
