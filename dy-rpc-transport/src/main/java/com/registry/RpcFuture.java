package com.registry;

import java.util.concurrent.CountDownLatch;

public class RpcFuture {
        private final CountDownLatch latch = new CountDownLatch(1);
        private byte[] response;

        public void complete(byte[] response) {
            this.response = response;
            latch.countDown();
        }

        public byte[] get() throws InterruptedException {
            latch.await(); // 阻塞直到收到响应
            return response;
        }
    }