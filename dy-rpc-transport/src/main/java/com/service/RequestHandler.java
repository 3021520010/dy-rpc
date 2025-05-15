package com.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

public interface RequestHandler {
    void onRequest(InputStream request, OutputStream response);
    void onRequest(InputStream request, SocketChannel channel);
}