package com.brimworks.nio;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Objects;

/**
 * Working with ByteBuffers directly is difficult since it is unclear when
 * a buffer is in "flush" mode (aka valid bytes are between position and mark)
 * and "fill" mode (aka valid bytes are between 0 and position).
 * 
 * Additionally, this abstraction requires unnecessary compact() calls which
 * are O(n) operations where n is the number of valid bytes in the buffer.
 * 
 * This class overcomes these issues by defining all the normal put() and
 * get() methods that you expect, but implemented in a ring buffer such that
 * put()ing off the "end" of the buffer results in writing to the beginning of
 * the buffer, assuming sufficient capacity remains.
 * 
 * To make this class interop with nio methods that require a ByteBuffer, use
 * the {@link RingByteBuffer#readFrom(ScatteringByteChannel)} method to acquire 
 * buffer(s) that are "ready" for a filling up with a read operation, or the
 * {@link RingByteBuffers#writeTo(GatheringByteChannel)} method to acquire buffer(s)
 * that are "ready" for flushing via a write operation. These methods are used
 * to "view" the internal ByteBuffer as 0 to 2 buffers. It may be two buffers if the
 * "view"ed bytes wrap around.
 */
public class RingByteBuffer implements Comparable<RingByteBuffer> {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final char SUBSTITUTE_CHAR = 0x2423;

    private ByteBuffer buff;
    private int begin; // offset where valid bytes begin.
    private int size;  // total number of valid bytes. Full if size == buff.capacity()

    /**
     * @param capacity is the max bytes that can be contained.
     * @return a new RingByteBuffer with nothing in it.
     */
    public static RingByteBuffer allocate(int capacity) {
        return new RingByteBuffer(ByteBuffer.allocate(capacity), 0, 0);
    }

    /**
     * Duplicates a buffer, maintaining valid bytes [0, position). Changes
     * to the ring buffer are visible to the byte buffer and visa versa.
     * @param buff is in "fill" mode (used during read() call).
     * @return a new RingByteBuffer.
     */
    public static RingByteBuffer wrap(ByteBuffer buff) {
        return new RingByteBuffer(buff.duplicate(), 0, buff.position());
    }

    /**
     * Duplicates a buffer, maintaining valid bytes between [position,
     * limit).  Changes to the ring buffer are visible to the byte buffer
     * and visa versa. Use this method instead of {@link #wrap(ByteBuffer))}
     * if the buffer
     * @param buff is in "flush" mode (used during write() call).
     * @return a new RingByteBuffer.
     */
    public static RingByteBuffer wrapFlipped(ByteBuffer buff) {
        return new RingByteBuffer(buff.duplicate(), buff.position(), buff.limit() - buff.position());
    }

    private RingByteBuffer(ByteBuffer buff, int begin, int size) {
        this.buff = buff;
        this.begin = begin;
        this.size = size;
    }

    /**
     * @return true if there are no valid bytes.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * @return true if there are no invalid bytes.
     */
    public boolean isFull() {
        return size == buff.capacity();
    }

    /**
     * @return the number of valid bytes.
     */
    public int size() {
        return size;
    }

    /**
     * @return the number of invalid bytes.
     */
    public int remainingCapacity() {
        return buff.capacity() - size;
    }

    /**
     * @return the total capacity of this ring buffer.
     */
    public int totalCapacity() {
        return buff.capacity();
    }

    /**
     * Mark all bytes as invalid.
     * @return this for easy method chaining.
     */
    public RingByteBuffer clear() {
        size = 0;
        begin = 0; // avoid unnecessary fragmentation when converting to ByteBuffer.
        return this;
    }

    /**
     * @return the offset within the backing ByteBuffer which
     *     represents the first valid byte.
     */
    public int getBeginOffset() {
        return begin;
    }

    /**
     * @param minCapacity is the minimal capacity
     * @return this if there is sufficient capacity, otherwise
     *    a new RingByteBuffer is allocated with max(2 * currentCapacity, minCapacity)
     *    and all existing valid bytes are transfered to the new RingByteBuffer.
     */
    public RingByteBuffer ensureCapacity(int minCapacity) {
        int capacity = buff.capacity();
        if (minCapacity < capacity) {
            return this;
        }
        if (minCapacity < 2 * capacity) {
            minCapacity = 2 * capacity;
        }
        RingByteBuffer result = RingByteBuffer.allocate(minCapacity);
        buff.position(begin);
        if (begin + size > capacity) {
            buff.limit(capacity);
            result.buff.put(buff);
            buff.position(0);
        }
        buff.limit((begin + size) % capacity);
        result.buff.put(buff);
        result.size = size;
        return result;
    }

    /**
     * @param channel is either ignored (if no valid bytes), or the
     *     {@link GatheringByteChannel#write(ByteBuffer)} method is called if valid
     *     bytes do not "wrap around", or the
     *     {@link GatheringByteChannel#write(ByteBuffer[])} method is called with an
     *     array of length 2.
     * @return the result from the write() method call or 0 if no valid bytes.
     * @throws IOException if channel write method throws.
     */
    public long writeTo(GatheringByteChannel channel) throws IOException {
        if (0 == size) {
            return 0;
        }
        int capacity = buff.capacity();
        buff.position(begin);
        if (begin + size <= capacity) {
            buff.limit(begin + size);
            return channel.write(buff.slice().asReadOnlyBuffer());
        }
        buff.limit(buff.capacity());
        ByteBuffer[] buffs = new ByteBuffer[] {
            buff.slice().asReadOnlyBuffer(),
            null
        };

        buff.position(0);
        buff.limit((begin + size) % capacity);
        buffs[1] = buff.slice().asReadOnlyBuffer();
        return channel.write(buffs);
    }

    /**
     * @param channel is either ignored (if no valid bytes), or the
     *     {@link ScatteringByteChannel#read(ByteBuffer)} method is called if invalid
     *     bytes do not "wrap around", or the
     *     {@link ScatteringByteChannel#read(ByteBuffer[])} method is called with an
     *     array of length 2.
     * @return the result from the read() method call or 0 if no invalid bytes.
     * @throws IOException if channel read method throws.
     * @throws ReadOnlyBufferException if this is a read-only buffer.
     */
    public long readFrom(ScatteringByteChannel channel) throws IOException {
        if (buff.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        int capacity = buff.capacity();
        if (capacity == size) {
            return 0;
        }
        int invalidBegin = (begin + size) % capacity;
        int invalidSize = capacity - size;
        buff.position(invalidBegin);
        if (invalidBegin + invalidSize <= capacity) {
            buff.limit(invalidBegin + invalidSize);
            return channel.read(buff.slice());
        }
        buff.limit(buff.capacity());
        ByteBuffer[] buffs = new ByteBuffer[] {
            buff.slice(),
            null
        };

        buff.position(0);
        buff.limit((invalidBegin + invalidSize) % capacity);
        buffs[1] = buff.slice();
        return channel.read(buffs);
    }

    /**
     * @return true if the underlying ByteBuffer is a direct buffer.
     * {@see ByteBuffer#isDirect()}
     */
    public boolean isDirect() {
        return buff.isDirect();
    }

    /**
     * @return true if the underlying ByteBuffer is a read only buffer.
     * {@see ByteBuffer#isReadOnly()}
     */
    public boolean isReadOnly() {
        return buff.isReadOnly();
    }

    /**
     * @return a new RingByteBuffer, copying the beginning offset, size, and
     *     shares the same backing store.
     */
    public RingByteBuffer duplicate() {
        return new RingByteBuffer(buff.duplicate(), begin, size);
    }

    /**
     * @return a new RingByteBuffer which is marked as read only,
     *    copies the beginning offset, size, and shares the same
     *    backing store.
     */
    public RingByteBuffer asReadOnlyBuffer() {
        return new RingByteBuffer(buff.asReadOnlyBuffer(), begin, size);
    }

    /**
     * @return true if the underlying ByteBuffer is backed by an array.
     * {@see ByteBuffer#hasArray()}
     */
    public boolean hasArray() {
        return buff.hasArray();
    }

    /**
     * @return the backing ByteBuffer's array, be careful to use {@link #arrayOffset()}
     *     in case this is a slice().
     * {@see ByteBuffer#array()}
     */
    public byte[] array() {
        return buff.array();
    }

    /**
     * @return the physical ByteBuffer's array offset, useful when using {@link #array()}
     * {@see ByteBuffer#arrayOffset()}
     */
    public int arrayOffset() {
        return buff.arrayOffset();
    }

    /**
     * @param order the new byte order to use.
     * @return this for method chaining.
     */
    public RingByteBuffer order(ByteOrder order) {
        buff.order(order);
        return this;
    }

    /**
     * @return the byte order used.
     */
    public ByteOrder order() {
        return buff.order();
    }

    /**
     * @return the first byte in this buffer, removing it from the buffer.
     * @throws BufferUnderflowException if the buffer is empty.
     */
    public byte get() {
        if (size <= 0) {
            throw new BufferUnderflowException();
        }
        buff.limit(buff.capacity());
        byte output = buff.get(begin);
        size--;
        begin = (begin + 1) % buff.capacity();
        return output;
    }

    /**
     * @param offset is the offset within the buffer to retrieve a byte.
     * @return the byte at the specified offset. Note that this
     *     offset will be modulus with the capacity.
     * <b>NOTE:</b> No check is performed on offset to see if it is valid. Instead,
     * use {@link #isValidOffset(int)} to determine if the offset is valid.
     */
    public byte get(int offset) {
        int capacity = buff.capacity();
        offset %= capacity;
        buff.limit(capacity);
        return buff.get(offset);
    }

    /**
     * @param offset is the offset to check for validity.
     * @return true if the offset is valid. Note that this offset
     *    will be modulous with the capacity.
     */
    public boolean isValidOffset(int offset) {
        int capacity = buff.capacity();
        offset %= capacity;
        if (offset >= begin) {
            return offset < (begin + size);
        }
        int end = (begin + size) % capacity;
        return offset < end;
    }

    /**
     * @param input is a byte to add to the end of the buffer.
     * @return this for method chaining.
     * @throws BufferOverflowException if there is insufficient capacity
     * @throws ReadOnlyBufferException if the buffer is marked as read only.
     */
    public RingByteBuffer put(byte input) {
        int capacity = buff.capacity();
        if (size >= capacity) {
            throw new BufferOverflowException();
        }
        buff.put((begin + size++) % capacity, input);
        return this;
    }

    /**
     * @param offset is the offset within the buffer to put a byte,
     *     if this is out of range it will be modulus with the capacity.
     * @param input is a byte to set at the given offset.
     * @return this for method chaining.
     * @throws ReadOnlyBufferException if the buffer is marked as read only.
     */
    public RingByteBuffer put(int offset, byte input) {
        buff.put(offset % buff.capacity(), input);
        return this;
    }

    /**
     * @param input is a ByteBuffer with valid bytes between position (inclusive)
     *     and limit (exclusive). If successful, the input's position will be set to
     *     limit.
     * @return this for method chaining.
     * @throws BufferOverflowException if there is insufficient capacity
     * @throws ReadOnlyBufferException if this ring buffer is marked as read only.
     */
    public RingByteBuffer putAll(ByteBuffer input) {
        int count = input.remaining();
        int capacity = buff.capacity();
        if (size + count >= capacity) {
            throw new BufferOverflowException();
        }
        int beginWrite = (begin + size) % capacity;
        if (beginWrite + count > capacity) {
            ByteBuffer slice = input.slice();
            int subCount = capacity - beginWrite;
            slice.limit(subCount);
            buff.position(beginWrite);
            buff.limit(capacity);
            buff.put(slice);
            input.position(input.position() + subCount);
            beginWrite = 0;
        }
        buff.position(beginWrite);
        buff.limit(capacity);
        buff.put(input);
        size += count;
        return this;
    }

    /**
     * Transfer up to input's remaining bytes, returning the actual number of
     * bytes transferred. Not all bytes will be transferred if there is unsufficient
     * capacity in this ring byte buffer.
     * @param input is a ByteBuffer with valid bytes between position (inclusive)
     *     and limit (exclusive). The input's position will be incremented by the
     *     number of bytes transferred.
     * @return the number of bytes transferred.
     * @throws ReadOnlyBufferException if this ring buffer is marked as read only.
     */
    public int putSome(ByteBuffer input) {
        int count = Math.min(input.remaining(), remainingCapacity());
        int capacity = buff.capacity();
        int beginWrite = (begin + size) % capacity;
        if (beginWrite + count > capacity) {
            ByteBuffer slice = input.slice();
            int subCount = capacity - beginWrite;
            slice.limit(subCount);
            buff.position(beginWrite);
            buff.limit(capacity);
            buff.put(slice);
            input.position(input.position() + subCount);
            beginWrite = 0;
        }
        buff.position(beginWrite);
        buff.limit(capacity);
        buff.put(input);
        size += count;
        return count;
    }

    /**
     * Fill up dst buffer between position and limit such that
     * the position of the dst buffer will be set to limit, and
     * all necessary bytes from the ring buffer are copied over,
     * and the internal ring buffer's beginning is incremented and
     * size decreased.
     *
     * @param dst is a buffer to fill up between position (inclusive)
     *     and limit (exclusive).
     * @return this for method chaining.
     * @throws ReadOnlyBufferException if the dst buffer is marked as read only.
     * @throws BufferUnderflowException if there is insufficient valid
     *     bytes in this ring buffer to satisfy this read request. See
     *     {@see #getSome(ByteBuffer)} to avoid this exception.
     */
    public RingByteBuffer getAll(ByteBuffer dst) {
        int count = dst.remaining();
        if (count > size) {
            throw new BufferUnderflowException();
        }
        int capacity = buff.capacity();
        if (begin + count > capacity) {
            ByteBuffer slice = dst.slice();
            int subCount = capacity - begin;
            slice.limit(subCount);
            buff.position(begin);
            buff.limit(capacity);
            slice.put(buff);
            dst.position(dst.position() + subCount);
            begin = 0;
            size -= subCount;
            count -= subCount;
        }
        buff.position(begin);
        buff.limit(begin + count);
        dst.put(buff);
        begin += count;
        size -= count;
        return this;
    }

    /**
     * Fill up dst buffer between position and limit such that
     * the position of the dst buffer will be set to limit, and
     * all necessary bytes from the ring buffer are copied over,
     * and the internal ring buffer's beginning is incremented and
     * size decreased.
     *
     * @param dst is a buffer to fill up between position (inclusive)
     *     and limit (exclusive).
     * @return this for method chaining.
     * @throws ReadOnlyBufferException if the dst buffer is marked as read only.
     * @throws BufferUnderflowException if there is insufficient valid
     *     bytes in this ring buffer to satisfy this read request. See
     *     {@see #getSome(ByteBuffer)} to avoid this exception.
     */
    public int getSome(ByteBuffer dst) {
        int count = dst.remaining();
        int finalCount = count;
        if (count > size) {
            count = size;
        }
        int capacity = buff.capacity();
        if (begin + count > capacity) {
            ByteBuffer slice = dst.slice();
            int subCount = capacity - begin;
            slice.limit(subCount);
            buff.position(begin);
            buff.limit(capacity);
            slice.put(buff);
            dst.position(dst.position() + subCount);
            begin = 0;
            size -= subCount;
            count -= subCount;
        }
        buff.position(begin);
        buff.limit(begin + count);
        dst.put(buff);
        begin += count;
        size -= count;
        return finalCount;
    }

    // TODO:
    // getChar() / putChar()
    // getDouble() / putDouble()
    // getFloat() / putFloat()
    // getInt() / putInt()
    // getLong() / putLong()
    // getShort() / putShort()

    // slice(offset, length)
    // support negative offset for "last N bytes" and size - N length.

    /**
     * Intended for debugging only! Do NOT rely on the output of this method!
     * @return a hexdump of the valid bytes.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int capacity = buff.capacity();
        buff.limit(capacity);
        int end = begin + size;
        for (int i = begin; i < end;) {
            sb.append(String.format("%04x ", i));
            int start = i;
            do {
                int ch = buff.get(i % capacity) & 0xFF;
                sb.append(" ");
                if (i % 16 == 8) {
                    sb.append(" ");
                }
                sb.append(HEX_ARRAY[ch >>> 4]);
                sb.append(HEX_ARRAY[ch & 0x0F]);
            } while (++i % 16 != 0 && i < end);
            if (i % 16 != 0) {
                sb.append(new String(new char[16 - i % 16]).replace("\0", "   "));
                if (i % 16 < 7) {
                    sb.append(" ");
                }
            }
            sb.append("  ");
            i = start;
            do {
                char ch = (char)buff.get(i % capacity);
                if (ch < 0x21) {
                    // Control chars:
                    switch (ch) {
                    case ' ':
                        sb.append((char)0x2420);
                        break;
                    case '\t':
                        sb.append((char)0x2409);
                        break;
                    case '\r':
                        sb.append((char)0x240D);
                        break;
                    case '\n':
                        sb.append((char)0x2424);
                        break;
                    default:
                        sb.append(SUBSTITUTE_CHAR);
                    }
                } else if (ch < 0x7F) {
                    sb.append(ch);
                } else {
                    // control chars:
                    sb.append(SUBSTITUTE_CHAR);
                }
            } while (++i % 16 != 0 && i < end);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public int compareTo(RingByteBuffer other) {
        int capacity = buff.capacity();
        buff.limit(capacity);
        int otherCapacity = other.buff.capacity();
        other.buff.limit(otherCapacity);
        int minSize = Math.min(size, other.size);
        for (int i = 0; i < minSize; i++) {
            int ch = buff.get((begin + i) % capacity);
            int otherCh = other.buff.get((other.begin + i) % otherCapacity);
            if (ch == otherCh) {
                continue;
            }
            return ch < otherCh ? -1 : 1;
        }
        if (size == other.size) {
            return 0;
        }
        return size < other.size ? -1 : 1;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RingByteBuffer)) {
            return false;
        }
        RingByteBuffer other = (RingByteBuffer)obj;
        if (size != other.size) {
            return false;
        }
        int capacity = buff.capacity();
        buff.limit(capacity);
        int otherCapacity = other.buff.capacity();
        other.buff.limit(otherCapacity);
        for (int i = 0; i < size; i++) {
            int ch = buff.get((begin + i) % capacity);
            int otherCh = other.buff.get((other.begin + i) % otherCapacity);
            if (ch == otherCh) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        try {
            return (int)writeTo(TO_HASH_CODE);
        } catch (IOException ex) {
            // Unreachable.
            throw new IllegalStateException(ex);
        }
    }

    private static final ToHashCode TO_HASH_CODE = new ToHashCode();
    private static class ToHashCode implements GatheringByteChannel {
        @Override
        public int write(ByteBuffer src) throws IOException {
            return Objects.hash(src);
        }

        @Override
        public boolean isOpen() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long write(ByteBuffer[] srcs) throws IOException {
            return Objects.hash(srcs[0], srcs[1]);
        }
    }
}