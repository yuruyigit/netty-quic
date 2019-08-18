package com.colingodsey.quic.packet;

import io.netty.buffer.ByteBuf;

import java.math.BigInteger;

import com.colingodsey.quic.packet.components.LongHeader;
import com.colingodsey.quic.packet.components.LongHeader.Type;
import com.colingodsey.quic.utils.VariableInt;

public class ZeroRTT implements Packet {
    final LongHeader header;
    final int packetNumber;
    final byte[] payload;

    public ZeroRTT(LongHeader header, ByteBuf in) {
        assert header.type == Type.ZERO_RTT;
        this.header = header;
        final int payloadLength = VariableInt.readInt(in);
        packetNumber = Packet.readFixedLengthInt(in, getPacketNumberBytes());
        payload = Packet.readBytes(in, payloadLength);
    }

    public ZeroRTT(int version, int packetNumber, byte[] payload,
            BigInteger sourceID, BigInteger destID) {
        final byte packetNumberBytes = (byte) (Packet.getFixedLengthIntBytes(packetNumber) - 1);
        this.header = new LongHeader(Type.ZERO_RTT, packetNumberBytes, version, sourceID, destID);
        this.packetNumber = packetNumber;
        this.payload = payload;
    }

    public void write(ByteBuf out) {
        header.write(out);
        VariableInt.write(payload.length, out);
        Packet.writeFixedLengthInt(packetNumber, getPacketNumberBytes(), out);
        out.writeBytes(payload);
    }

    public int getPacketNumberBytes() {
        return header.header + 1;
    }
}