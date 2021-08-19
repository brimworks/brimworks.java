package com.brimworks.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * Used for testing, all writes() are placed into internal buffer,
 * all reads() are removed from the internal buffer.
 */
public class ByteBufferChannel implements GatheringByteChannel, ScatteringByteChannel {
    private RingByteBuffer buff;
    private boolean isOpen = true;

    public ByteBufferChannel(RingByteBuffer buff) {
        this.buff = buff;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen) {
            throw new ClosedChannelException();
        }
        buff = buff.ensureCapacity(buff.size() + src.remaining());
        return buff.putSome(src);
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() throws IOException {
        isOpen = false;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!isOpen) {
            throw new ClosedChannelException();
        }
        return buff.getSome(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (!isOpen) {
            throw new ClosedChannelException();
        }
        long total = 0;
        for (; offset < length && !buff.isEmpty(); offset++) {
            total += buff.getSome(dsts[offset]);

        }
        return total;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        if (!isOpen) {
            throw new ClosedChannelException();
        }
        long total = 0;
        for (int offset = 0; offset < dsts.length && !buff.isEmpty(); offset++) {
            total += buff.getSome(dsts[offset]);
        }
        return total;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (!isOpen) {
            throw new ClosedChannelException();
        }
        long total = 0;
        for (; offset < length && !buff.isFull(); offset++) {
            total += buff.putSome(srcs[offset]);
        }
        return total;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        if (!isOpen) {
            throw new ClosedChannelException();
        }
        long total = 0;
        for (int offset = 0; offset < srcs.length && !buff.isFull(); offset++) {
            total += buff.putSome(srcs[offset]);
        }
        return total;
    }

    public RingByteBuffer getBuffer() {
        return buff;
    }
}
