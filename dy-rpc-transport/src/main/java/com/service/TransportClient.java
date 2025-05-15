package com.service;

import java.io.InputStream;

/**
 * 1.创建链接
 * 2.发送数据 并且等待响应
 * 3.关闭链接
 */
public interface TransportClient {
    InputStream write(InputStream data);
    void close();
}
