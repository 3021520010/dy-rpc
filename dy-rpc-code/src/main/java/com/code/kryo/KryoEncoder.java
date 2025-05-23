package com.code.kryo;

import com.code.service.Encoder;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayOutputStream;

public class KryoEncoder implements Encoder {

    @Override
    public byte[] encode(Object obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        Kryo kryo = kryoThreadLocal.get();
        kryo.writeObject(output, obj); // 或者用 writeClassAndObject
        output.close();
        return baos.toByteArray();
    }

    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        return kryo;
    });

}
