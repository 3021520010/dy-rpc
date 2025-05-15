package com.code.json;

import com.alibaba.fastjson.JSON;
import com.code.service.Decoder;

/**
 * 基于JSON的解码器
 */
public class JSONDecoder implements Decoder {
    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        return JSON.parseObject(bytes,clazz);
    }
}
