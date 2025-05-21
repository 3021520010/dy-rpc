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
import org.springframework.util.Assert;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@Slf4j
public class RemoteInvoker implements InvocationHandler {
    private Class clazzz;
    private Encoder encoder;
    private Decoder decoder;
    private ServiceRegistry registry;
    private LoadBalancer loadBalancer;
    TransportClient client = null;
    //默认3
    int retryCount = 3;
    int retryTime=1000;
    String hashKey;
    public RemoteInvoker(Class clazzz,Encoder encoder,Decoder decoder,ServiceRegistry serviceRegistry,LoadBalancer loadBalancer,int retryCount,int retryTime,String key){
        this.decoder=decoder;
        this.encoder=encoder;
        this.loadBalancer=loadBalancer;
        this.registry=serviceRegistry;
        this.clazzz=clazzz;
        client = new NioTransportClient();
        this.retryTime=retryTime;
        this.retryCount=retryCount;
        hashKey=key;
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
        Response response = new Response();

        List<InetSocketAddress> activeAddress = registry.lookup(clazzz.getName());

        for (int i = 0; i < retryCount; i++) {
            try {
                Assert.notNull(activeAddress, "当前没有可用的服务端地址");
                //负载均衡选择一个
                InetSocketAddress address = loadBalancer.select(activeAddress,hashKey);
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
                log.info("客户端接收到信息{}",response);
                // 如果响应成功，则返回
                if (response != null && response.getCode() == 0) {
                    return response;
                } else {
                    //切换服务端地址
                    activeAddress.remove(address);
                    log.error("调用失败，第 " + (i + 1) + " 次，服务端地址:{},服务端返回错误{} ", activeAddress,response);
                }

            } catch (Exception e) {
                log.error("调用失败，第 " + (i + 1) + " 次，异常: {}" , e.getMessage());
                response.setCode(1);
                response.setMessage("RPC 调用异常: " + e.getMessage());
            }
            // 失败后等待 1 秒
            try {
                Thread.sleep(retryTime);
            } catch (InterruptedException ignored) {
            }
        }
        // 多次重试仍然失败
        return response;
    }
}
