package com.protocol;

import lombok.Data;

/**
 * 表示RPC的返回结果
 */
@Data
public class Response {
    /**
     * 状态码 0 表示成功，1表示失败
     */
    private int code=0;
    /**
     * 错误信息
     */
    private String message="ok";
    /**
     * 返回结果
     */
    private Object data;
}
