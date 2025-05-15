package com.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示一个RPC请求 存的请求参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Request {
    private ServiceDescriptor service;
    private Object[] parameters;
}
