package com.invoker;


import com.code.service.Decoder;
import com.code.service.Encoder;
import com.protocol.Request;
import com.protocol.Response;
import com.protocol.ServiceDescriptor;
import com.service.TransportClient;
import com.service.TransportSelector;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.InvocationHandler;

@Slf4j
public class RemoteInvoker implements InvocationHandler {
    private Class clazzz;
    private Encoder encoder;
    private Decoder decoder;
    private TransportSelector selector;
    public RemoteInvoker(Class clazzz,Encoder encoder,Decoder decoder,TransportSelector transportSelector){
        this.decoder=decoder;
        this.encoder=encoder;
        this.selector=transportSelector;
        this.clazzz=clazzz;
    }
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
        Request request=new Request();
        request.setService(ServiceDescriptor.from(clazzz,method));
        request.setParameters(args);
        Response response=invokeRemote(request);
        log.error(response.toString());
        if(response==null||response.getCode()!=0){
            throw new IllegalStateException("fail to invoke remote: "+response);
        }
        return response.getData();
    }

    private Response invokeRemote(Request request) {
        Response response = new Response();
        TransportClient client = null;
        try {
            client = selector.select();
            // 写请求
            byte[] requestBytes = encoder.encode(request);
            // ✅ 加上4字节长度头
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            //dos.writeInt(requestBytes.length);
            dos.write(requestBytes);
            dos.flush();
            InputStream in = new ByteArrayInputStream(bos.toByteArray());
// 调用
            InputStream res = client.write(in);
// 读取响应并反序列化（假设服务端也返回 Response 对象）
            byte[] respBytes = res.readAllBytes();
            response  = decoder.decode(respBytes, Response.class);
            System.out.println("Client got response: " + response);
            client.close();
            return response;
        } catch (IOException e) {
            log.warn("fail to invoke remote", e);
            response.setCode(1);
            response.setMessage("RpcClient got error: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                selector.release(client);
            }
        }
    }
}
