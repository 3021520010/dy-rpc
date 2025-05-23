package com.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 表示一个RPC请求 存的请求参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Request implements Serializable {
    private ServiceDescriptor service;
    private Object[] parameters;
}
