package com.colingodsey.quic.packet.frame;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

import java.util.function.Consumer;

import com.colingodsey.quic.packet.Packet;
import com.colingodsey.quic.packet.frame.Frame.Orderable;
import com.colingodsey.quic.utils.VariableInt;

public final class Crypto extends AbstractReferenceCounted implements Frame,
        Frame.Initial, Frame.Handshake, Orderable {
    public static final int PACKET_ID = 0x06;

    private final long offset;
    private final ByteBuf payload;
    private final Packet.Type level;

    public Crypto(Packet.Type level, ByteBuf payload) {
        this.offset = -1;
        this.payload = payload;
        this.level = level;
    }

    private Crypto(ByteBuf in, Packet.Type level) {
        Frame.verifyPacketId(in, PACKET_ID);
        offset = VariableInt.read(in);
        payload = in.readRetainedSlice(VariableInt.readInt(in));
        this.level = level;
    }

    private Crypto(long offset, ByteBuf payload, Packet.Type level) {
        this.offset = offset;
        this.payload = payload;
        this.level = level;
    }

    public static Crypto read(ByteBuf in, Packet.Type level) {
        return new Crypto(in, level);
    }

    public int length() {
        return 1 + VariableInt.length(offset) +
                VariableInt.length(payload.readableBytes()) + payload.readableBytes();
    }

    public long splitAndOrder(long offset, int maxLength, Consumer<Crypto> out) {
        assert offset >= 0;
        final ByteBuf tmp = payload.duplicate();
        while (tmp.isReadable()) {
            final int length = Math.min(maxLength, tmp.readableBytes());
            out.accept(new Crypto(offset, tmp.readRetainedSlice(length), level));
            offset += length;
        }
        return offset;
    }

    public void write(ByteBuf out) {
        assert offset != -1;
        VariableInt.write(PACKET_ID, out);
        VariableInt.write(offset, out);
        VariableInt.write(getPayloadLength(), out);
        out.writeBytes(payload);
    }

    public void writePayload(ByteBuf out) {
        final int readerIndex = payload.readerIndex();
        out.writeBytes(payload);
        payload.readerIndex(readerIndex);
    }

    public ByteBuf retainPayload() {
        return payload.retain();
    }

    public int getPayloadLength() {
        return payload.readableBytes();
    }

    public long getOffset() {
        return offset;
    }

    public Packet.Type getLevel() {
        return level;
    }

    public ReferenceCounted touch(Object hint) {
        payload.touch(hint);
        return this;
    }

    protected void deallocate() {
        payload.release();
    }
}
