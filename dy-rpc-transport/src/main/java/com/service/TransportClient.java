package com.service;

import com.protocol.Peer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 1.创建链接
 * 2.发送数据 并且等待响应
 * 3.关闭链接
 */
public interface TransportClient {
    void init(Peer peer);
    InputStream write(InputStream data);
    void close();
    CompletableFuture<InputStream> sendAsync(InputStream data) ;
}
