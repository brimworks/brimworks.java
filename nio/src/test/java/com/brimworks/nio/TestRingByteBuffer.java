package com.brimworks.nio;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestRingByteBuffer {

    /**
     * @return a buffer with valid bytes 1 to 5, but a begin offset
     *    of 2. This buffer is full (max capacity reached).
     */
    private static RingByteBuffer newWrappedBuffer() {
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        // Fill it up:
        buff.put((byte)0);
        buff.put((byte)0);
        buff.put((byte)1);
        buff.put((byte)2);
        buff.put((byte)3);
        // Ensure wrapping, so zeros are removed:
        buff.get();
        buff.get();
        buff.put((byte)4);
        buff.put((byte)5);
        return buff;
    }

    /**
     * @return a buffer with valid bytes 1 to 5. This buffer is full (max capacity reached).
     */
    private static RingByteBuffer newBuffer() {
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        buff.put((byte)1);
        buff.put((byte)2);
        buff.put((byte)3);
        buff.put((byte)4);
        buff.put((byte)5);
        return buff;
    }

    @Test
    public void testWrap() {
        ByteBuffer buff = ByteBuffer.allocate(10);
        buff.put("abcd".getBytes(UTF_8));
        RingByteBuffer ringBuff = RingByteBuffer.wrap(buff);
        assertEquals('a', ringBuff.get());
        assertEquals('b', ringBuff.get());
        assertEquals('c', ringBuff.get());
        assertEquals('d', ringBuff.get());
        assertThrows(BufferUnderflowException.class, ringBuff::get);
    }

    @Test
    public void testWrapFlipped() {
        ByteBuffer buff = ByteBuffer.allocate(10);
        buff.put("xxxabcd".getBytes(UTF_8));
        buff.flip();
        buff.get(); buff.get(); buff.get();
        RingByteBuffer ringBuff = RingByteBuffer.wrapFlipped(buff);
        assertEquals('a', ringBuff.get());
        assertEquals('b', ringBuff.get());
        assertEquals('c', ringBuff.get());
        assertEquals('d', ringBuff.get());
        assertThrows(BufferUnderflowException.class, ringBuff::get);
    }

    @Test
    public void testEmpty() {
        RingByteBuffer buff = RingByteBuffer.allocate(2);
        assertTrue(buff.isEmpty());
        buff.put((byte)1);
        assertFalse(buff.isEmpty());
        buff.put((byte)2);
        assertFalse(buff.isEmpty());
        assertEquals(1, buff.get());
        assertFalse(buff.isEmpty());
        assertEquals(2, buff.get());
        assertTrue(buff.isEmpty());
    }

    @Test
    public void testFull() {
        RingByteBuffer buff = RingByteBuffer.allocate(2);
        assertFalse(buff.isFull());
        buff.put((byte)1);
        assertFalse(buff.isFull());
        buff.put((byte)2);
        assertTrue(buff.isFull());
        assertEquals(1, buff.get());
        assertFalse(buff.isFull());
        assertEquals(2, buff.get());
        assertFalse(buff.isFull());
    }

    @Test
    public void testCapacity() {
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        assertEquals(5, buff.remainingCapacity());
        assertEquals(5, buff.totalCapacity());
        buff.put((byte)1);
        buff.put((byte)2);
        buff.put((byte)3);
        assertEquals(2, buff.remainingCapacity());
        assertEquals(5, buff.totalCapacity());
        assertEquals(1, buff.get());
        assertEquals(2, buff.get());
        assertEquals(4, buff.remainingCapacity());
        assertEquals(5, buff.totalCapacity());
        buff.put((byte)4);
        buff.put((byte)5);
        buff.put((byte)6);
        assertEquals(1, buff.remainingCapacity());
        assertEquals(5, buff.totalCapacity());
    }

    @Test
    public void testClear() {
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        buff.put((byte)1);
        buff.put((byte)2);
        buff.put((byte)3);
        buff.clear();
        assertThrows(BufferUnderflowException.class, buff::get);
    }

    @Test
    public void testBeginOffset() {
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        assertEquals(0, buff.getBeginOffset());
        buff.put((byte)1);
        assertEquals(0, buff.getBeginOffset());
        buff.put((byte)2);
        assertEquals(0, buff.getBeginOffset());
        buff.put((byte)3);
        assertEquals(1, buff.get());
        assertEquals(1, buff.getBeginOffset());
        assertEquals(2, buff.get());
        assertEquals(2, buff.getBeginOffset());
        buff.put((byte)4);
        assertEquals(2, buff.getBeginOffset());
        buff.put((byte)5);
        assertEquals(2, buff.getBeginOffset());
        buff.put((byte)6);
        assertEquals(3, buff.get());
        assertEquals(3, buff.getBeginOffset());
        assertEquals(4, buff.get());
        assertEquals(4, buff.getBeginOffset());
        assertEquals(5, buff.get());
        assertEquals(0, buff.getBeginOffset());
        assertEquals(6, buff.get());
        // Technically it is indeterminate:
        assertEquals(1, buff.getBeginOffset());
    }

    @Test
    public void testEnsureCapacity() {
        RingByteBuffer buff = newWrappedBuffer();
        RingByteBuffer newBuff = buff.ensureCapacity(11);
        // Should be a new instance:
        assertTrue(buff != newBuff);
        // But logically equal:
        assertEquals(newBuff, buff);
        // And specific capacity:
        assertEquals(newBuff.totalCapacity(), 11);

        RingByteBuffer newNewBuff = newBuff.ensureCapacity(12);
        // Ensure doubling was choosen instead:
        assertEquals(newNewBuff.totalCapacity(), 22);
        assertEquals(newNewBuff, buff);

        // Ensure nothing happens if sufficient capacity is available:
        RingByteBuffer alias = newBuff.ensureCapacity(10);
        assertTrue(alias == newBuff);
    }

    @Test
    public void testWriteToWrapped() throws IOException {
        ByteBufferChannel channel = new ByteBufferChannel(RingByteBuffer.allocate(10));
        RingByteBuffer buff = newWrappedBuffer();
        assertEquals(5L, buff.writeTo(channel));
        assertEquals(0, buff.size());
        assertEquals(newBuffer(), channel.getBuffer());
    }

    @Test
    public void testWriteTo() throws IOException {
        ByteBufferChannel channel = new ByteBufferChannel(RingByteBuffer.allocate(10));
        RingByteBuffer buff = newBuffer();
        assertEquals(5L, buff.writeTo(channel));
        assertEquals(0, buff.size());
        assertEquals(newBuffer(), channel.getBuffer());
    }

    @Test
    public void testWriteToEmpty() throws IOException {
        ByteBufferChannel channel = new ByteBufferChannel(RingByteBuffer.allocate(10));
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        assertEquals(0, buff.writeTo(channel));
    }

    @Test
    public void testReadFromWrapped() throws IOException {
        ByteBufferChannel channel = new ByteBufferChannel(newBuffer());
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        // Advance the offset:
        buff.put((byte)0); buff.put((byte)0); buff.get(); buff.get();
        assertEquals(5L, buff.readFrom(channel));
        assertEquals(0, channel.getBuffer().size());
        assertEquals(newBuffer(), buff);
    }

    @Test
    public void testReadFrom() throws IOException {
        ByteBufferChannel channel = new ByteBufferChannel(newBuffer());
        RingByteBuffer buff = RingByteBuffer.allocate(5);
        assertEquals(5L, buff.readFrom(channel));
        assertEquals(0, channel.getBuffer().size());
        assertEquals(newBuffer(), buff);
    }

    @Test
    public void testReadFromNoCapacity() throws IOException {
        ByteBufferChannel channel = new ByteBufferChannel(newBuffer());
        assertEquals(0, newBuffer().readFrom(channel));
    }

    @Test
    public void testReadFromReadonly() throws IOException {
        ByteBufferChannel channel = new ByteBufferChannel(newBuffer());
        RingByteBuffer buff = newBuffer().asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> {
            buff.readFrom(channel);
        });
    }

    @Test
    public void testDirect() {
        assertTrue(RingByteBuffer.wrap(ByteBuffer.allocateDirect(2)).isDirect());
        assertFalse(RingByteBuffer.wrap(ByteBuffer.allocate(2)).isDirect());
    }

    @Test
    public void testReadOnly() {
        assertTrue(RingByteBuffer.wrap(ByteBuffer.allocate(2).asReadOnlyBuffer()).isReadOnly());
        assertFalse(RingByteBuffer.wrap(ByteBuffer.allocate(2)).isReadOnly());
        assertTrue(RingByteBuffer.wrap(ByteBuffer.allocate(2)).asReadOnlyBuffer().isReadOnly());
    }

    @Test
    public void testDuplicate() {
        RingByteBuffer buff = newWrappedBuffer();
        RingByteBuffer duplicated = buff.duplicate();
        assertEquals(1, buff.get());
        assertEquals(2, buff.get());
        assertEquals(3, buff.get());
        assertEquals(4, buff.get());
        buff.put((byte)6);
        buff.put((byte)7);
        assertEquals(6, duplicated.get());
        assertEquals(7, duplicated.get());
        assertEquals(3, duplicated.get());
        assertEquals(4, duplicated.get());
        assertEquals(5, duplicated.get());
    }

    // TODO: Finish implementing these tests.
    @Test
    public void testArray() {
        // hasArray()
        // array()
        // arrayOffset()
    }
 
    @Test
    public void testOrder() {
        // order(ByteOrder)
        // order()
    }

    @Test
    public void testGet() {
        // get()
        // get(offset)
        // isValidOffset(offset)
    }

    @Test
    public void testPut() {
        // put(ch)
        // put(offset, ch)
    }

    @Test
    public void testPutAll() {
        // putAll(ByteBuffer)
    }

    @Test
    public void testPutSome() {
        // putSome(ByteBuffer)
    }

    @Test
    public void testGetAll() {
        // getAll(ByteBuffer)
    }

    @Test
    public void testGetSome() {
        // getSome(ByteBuffer)
    }

    @Test
    public void testToString() {
        // toString()
    }

    @Test
    public void testCompareTo() {
        // compareTo(other)
    }

    @Test
    public void testEquals() {
        // equals()
    }

    @Test
    public void testHashCode() {
        // hashCode()
    }
}