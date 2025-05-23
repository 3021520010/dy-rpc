package com.code.hessian;

import com.code.service.Decoder;
import com.caucho.hessian.io.HessianInput;

import java.io.ByteArrayInputStream;

public class HessianDecoder implements Decoder {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            HessianInput input = new HessianInput(bis);
            Object obj = input.readObject();
            return (T) obj;
        } catch (Exception e) {
            throw new RuntimeException("Hessian decode error", e);
        }
    }
}
