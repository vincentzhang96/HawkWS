/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Vincent Zhang/PhoenixLAB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.divinitor.dn.net.hawk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PacketTapServer {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ServerSocket serverSocket;
    private int port;
    private ExecutorService executorService;
    private List<DnClientHandler> handlers;
    private final Consumer<DnPacket> packetHandler;

    public PacketTapServer(Consumer<DnPacket> packetHandler) {
        this.packetHandler = packetHandler == null ? p -> {} : packetHandler;
        this.executorService = Executors.newCachedThreadPool();
        this.handlers = new ArrayList<>();
    }

    public void start(int port) {
        this.port = port;
        this.executorService.submit(this::run);
    }

    private void run() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            LOGGER.info("Listing for DN connections on port {}", this.port);
            while (true) {
                Socket clientSocket = this.serverSocket.accept();
                DnClientHandler handler = new DnClientHandler(clientSocket, this, this.packetHandler);
                this.executorService.submit(handler);
                this.handlers.add(handler);
            }

        } catch (InterruptedIOException ignored) {
        } catch (Exception e) {
            LOGGER.warn("Exception while listening for DN connections", e);
        }
    }

    public void stop() {
        this.executorService.shutdownNow();
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
