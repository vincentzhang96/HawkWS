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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;

public class Hawk implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private HawkWebsocketServer websocketServer;
    private PacketTapServer tapServer;

    public Hawk() {

    }

    public static void main(String[] args) {
        Hawk hawk = new Hawk();
        hawk.run();
    }

    @Override
    public void run() {
        LOGGER.info("Starting Hawk DN PacketTap server...");

        this.websocketServer = new HawkWebsocketServer(new InetSocketAddress("127.0.0.1", 14301));
        this.websocketServer.start();

        this.tapServer = new PacketTapServer(this::onPacket);
        this.tapServer.start(14300);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        LOGGER.info("Enter 'stop' to stop the server");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            System.out.println(line);
            if (line.equalsIgnoreCase("stop")) {
                break;
            }
        }
    }

    private void stop() {
        LOGGER.info("Server stopping");

        if (this.tapServer != null) {
            this.tapServer.stop();
        }

        if (this.websocketServer != null) {
            try {
                this.websocketServer.stop();
            } catch (IOException e) {
                LOGGER.warn("Closing", e);
            } catch (InterruptedException ignored) {
            }
        }

        LOGGER.info("Stopped");
    }

    private void onPacket(DnPacket packet) {
        byte[] data = packet.getData();
        byte[] buf = new byte[data.length + 1 + 4];
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) packet.getDirection().ordinal());
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        this.websocketServer.broadcast(buf);
    }
}
