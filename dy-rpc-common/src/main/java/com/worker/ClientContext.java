package com.worker;

import lombok.Data;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

@Data
public class ClientContext {
        ByteBuffer lenBuffer = ByteBuffer.allocate(4);
        ByteBuffer dataBuffer;
        boolean readingLen = true;
        CompletableFuture<InputStream> future;
        public ClientContext(CompletableFuture<InputStream> future) {
            this.future = future;
        }
    }