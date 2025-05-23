package com.code.enums;

import com.code.hessian.HessianDecoder;
import com.code.hessian.HessianEncoder;
import com.code.json.JSONDecoder;
import com.code.json.JSONEncoder;
import com.code.kryo.KryoDecoder;
import com.code.kryo.KryoEncoder;
import com.code.service.Encoder;
import com.code.service.Decoder;

public enum TransportCodeType {
        JSON("JSON", JSONEncoder.class, JSONDecoder.class),
        HESSIAN("Hessian", HessianEncoder.class, HessianDecoder.class),
        KYRO("Kyro",KryoEncoder.class, KryoDecoder.class);
        private final String typeName;
        private final Class<? extends Encoder> encoderClass;
        private final Class<? extends Decoder> decoderClass;
        TransportCodeType(String typeName,
                          Class<? extends Encoder> encoderClass,
                          Class<? extends Decoder> decoderClass) {
            this.typeName = typeName;
            this.encoderClass = encoderClass;
            this.decoderClass = decoderClass;
        }

        public String getTypeName() {
            return typeName;
        }

        public Class<? extends Encoder> getEncoderInstance() {
           return encoderClass;
        }

        public Class<? extends Decoder> getDecoderInstance() {
           return decoderClass;
        }

    }