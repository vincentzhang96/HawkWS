# Divinitor HawkWS Server

This is a basic Divinitor Hawk to Websocket server implemented in Java.

The server listens for Hawk connections on port 14300 and the websocket port server on 14301.

# Building and Running

This project uses Maven. Build by using

```
mvn install
```

You can run the resulting JAR as any other (`java -jar NAME_OF_JAR`) or

```
mvn exec:exec
```

# What is Hawk?

Hawk is a basic *packet exfiltration* system for allowing inspection of 
network packets. A target application is *tapped* by some means (as a 
feature, injected via DLL/plugin, etc) which sends the data, wrapped in 
a common protocol, to a listening Hawk server, which then translates the 
payload for external consumption.

This implementation serves a websocket server and broadcasts packets 
to all connected websockets.

# Hawk Protocol

Hawk is designed to be *endian-detecting*, meaning that either little or 
big endian may be used. The protocol provides a means to detect the 
endian of each and requires implementations to switch accordingly.

## Tap to Server

### Handshake

| Version | Magic Value |
|---------|-------------|
| 1 | 0x44332211 |

Upon connecting to the Hawk server, a packet tap must send the magic value corresponding to the protocol version. The server will switch to the matching endian-ness based on this magic value.

### Payload

| Size | Name | Description |
|------|------|-------------|
| 1 | Type | The payload type. 0 is Client->Server, 1 is Server->Client, and other values are implementation specific or considered an error value |
| 4 | Payload Size | The size of the payload (packet data) |
| n | Payload | The payload (packet data) |

## Server to Tap

No upstream communication is supported at the moment, but may be 
in future versions.
