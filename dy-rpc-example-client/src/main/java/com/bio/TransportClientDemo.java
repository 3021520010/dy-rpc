package com.bio;

import com.bio.BioTransportClient;
import com.service.TransportClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class TransportClientDemo
{
    // 启动客户端
    public static void main(String[] args) {
        try {
            TransportClient client = new BioTransportClient("localhost", 3000);
            String request = "Hello RPC";
            InputStream in = new ByteArrayInputStream(request.getBytes());
            InputStream response = client.write(in);
            System.out.println("Client got response: " + new String(response.readAllBytes()));
            client.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
