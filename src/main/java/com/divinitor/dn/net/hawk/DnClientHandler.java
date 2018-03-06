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

import com.google.common.io.LittleEndianDataInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataInputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

public class DnClientHandler implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Socket socket;
    private final PacketTapServer server;
    private final Consumer<DnPacket> packetHandler;

    public DnClientHandler(Socket socket, PacketTapServer server, Consumer<DnPacket> packetHandler) {
        this.socket = socket;
        this.server = server;
        this.packetHandler = packetHandler == null ? (p) -> {} : packetHandler;
    }

    @Override
    public void run() {
        LOGGER.info("Accepted connection!");
        try (DataInputStream dataInputStream = new DataInputStream(this.socket.getInputStream())) {
            DataInput dis = dataInputStream;
            //  Endian detect
            int detect = dis.readInt();
            LOGGER.debug(String.format("Network flag 0x%08X", detect));
            if (detect != 0x44332211) {
                dis = new LittleEndianDataInputStream(dataInputStream);
                LOGGER.info("Detected little-endian stream");
            }

            PacketDirection type;
            do {
                type = PacketDirection.valueOf(dis.readUnsignedByte());
                if (type == PacketDirection.ERROR) {
                    LOGGER.info("End of stream");
                    return;
                }

                int wrappedLength = dis.readInt();
                LOGGER.debug(String.format("Read %s packet payload %,d bytes", type.toString(), wrappedLength));
                byte[] buf = new byte[wrappedLength];
                dis.readFully(buf);

                DnPacket packet = new DnPacket(type, buf);
                this.packetHandler.accept(packet);
            } while (type != PacketDirection.ERROR);

        } catch (SocketException se) {
            LOGGER.warn("Socket error: {}", se.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOGGER.info("Connection closed");
        }
    }
}
