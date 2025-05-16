package com.service;

import com.protocol.Peer;

import java.util.List;

/**
 * 连接池选择哪个服务去连接
 */
public interface TransportSelector {
    /**
     * 初始化可以连接的server端点信息
     * @param peers
     * @param count
     * @param clazz
     */
    void init(List<Peer> peers, int count, Class<? extends TransportClient> clazz);
    /**
     * 选择一个连接
     * @return
     */
    TransportClient select();

    /**
     * 释放用完的client
     * @param client
     */
    void release(TransportClient client);

    /**
     *
     */
    void close();
}
