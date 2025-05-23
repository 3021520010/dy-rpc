package com.code.hessian;

import com.code.service.Encoder;
import com.caucho.hessian.io.HessianOutput;

import java.io.ByteArrayOutputStream;

public class HessianEncoder implements Encoder {

    @Override
    public byte[] encode(Object obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            HessianOutput output = new HessianOutput(bos);
            output.writeObject(obj);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Hessian encode error", e);
        }
    }
}
