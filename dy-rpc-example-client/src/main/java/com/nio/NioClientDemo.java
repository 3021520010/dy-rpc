package com.nio;

import com.bio.BioTransportClient;
import com.code.json.JSONDecoder;
import com.code.json.JSONEncoder;
import com.code.service.Decoder;
import com.code.service.Encoder;
import com.protocol.Request;
import com.protocol.Response;
import com.protocol.ServiceDescriptor;
import com.service.TransportClient;
import com.service.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;

public class NioClientDemo {

    // 启动客户端
    public static void main(String[] args) {
        try {
            Decoder decoder = new JSONDecoder();
            Encoder encoder = new JSONEncoder();
            TransportClient client = new NioTransportClient("localhost", 3000);

            Request request = new Request();
            ServiceDescriptor serviceDescriptor = ServiceDescriptor.from(test.class, test.class.getMethod("add", int.class, int.class));
            request.setService(serviceDescriptor);
            Object[] arg = new Object[]{1, 2};
            request.setParameters(arg);

// ✅ 正确：序列化整个 Request，而不是参数
            byte[] requestBytes = encoder.encode(request);

// ✅ 加上4字节长度头
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            //dos.writeInt(requestBytes.length);
            dos.write(requestBytes);
            dos.flush();

            InputStream in = new ByteArrayInputStream(bos.toByteArray());

// 调用
            InputStream response = client.write(in);

// 读取响应并反序列化（假设服务端也返回 Response 对象）
            byte[] respBytes = response.readAllBytes();
            Response resp = decoder.decode(respBytes, Response.class);
            System.out.println("Client got response: " + resp);

            client.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
