package com.code.kryo;

import com.code.service.Decoder;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import java.io.ByteArrayInputStream;

public class KryoDecoder implements Decoder {

    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false); // 可改为 true 并注册类提升性能
        return kryo;
    });

    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Input input = new Input(bais);
        Kryo kryo = kryoThreadLocal.get();
        Object obj = kryo.readObject(input, clazz);
        input.close();
        return clazz.cast(obj);
    }
}
