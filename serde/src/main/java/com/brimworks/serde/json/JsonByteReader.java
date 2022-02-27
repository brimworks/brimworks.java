package com.brimworks.serde.json;

import com.brimworks.serde.SerdeReader;
import com.brimworks.serde.SerdeEvent;
import com.brimworks.serde.SerdeNumber;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * 
 */
public class JsonByteReader extends JsonByteReaderBase implements SerdeReader {
    private static enum Expect {
        ANY,
        OBJECT_KEY,
        COMMA,
        COLON,
        DONE;
    }
    private static final byte OBJECT = 1;
    private static final byte ARRAY  = 2;

    private Expect expect = Expect.ANY;
    private long baseOffset;
    private SerdeEvent lastEvent;
    private String lastError;
    private byte[] stack = new byte[2048];
    private int stackTop = -1;

    public JsonByteReader() {
        this.stringParser = new StringParser();
        this.numberParser = new JsonNumber();
        init();
    }

    /**
     * Reset all internal state.
     */
    public void reset() {
        expect = Expect.ANY;
        baseOffset = 0;
        input = null;
        lastEvent = null;
        lastError = null;
        stackTop = -1;
        init();
    }

    /**
     * Feed the next ByteBuffer input.
     * @param input in "get" mode from which to read from. Subsequent calls MUST
     *     preserve any remaining bytes in this buffer, or call reset() to reset
     *     the internal state.
     * @param done set to true if end of stream should be signaled.
     */
    public void feed(ByteBuffer input, boolean done) {
        assert null != input;
        this.input = input;

        p = input.position();
        pe = input.limit();
        if (done) {
            eof = pe;
        }
    }

    /**
     * Set the maximum allowed JSON nesting depth. Default is 2048.
     * @param newDepth the new depth, must be greater than 0.
     */
    public void setMaxDepth(int newDepth) {
        assert newDepth > 0;
        byte[] newStack = new byte[newDepth];
        if (stackTop >= 0) {
            System.arraycopy(stack, 0, newStack, 0, stackTop + 1);
        }
        stack = newStack;
    }

    @Override
    public SerdeEvent read() {
        if (null == input || p == pe) {
            return p == eof ? SerdeEvent.EOF : SerdeEvent.UNDERFLOW;
        }
        SerdeEvent event = exec();
        event = handle(event);
        if (SerdeEvent.UNDERFLOW != event) {
            lastEvent = event;
        }
        return event;
    }

    @Override
    public int getLineNumber() {
        return line;
    }

    @Override
    public int getColumnNumber() {
        return ts - pline + 1;
    }

    @Override
    public long getOffset() {
        return baseOffset + ts;
    }

    @Override
    public boolean inObject() {
        return stackTop >= 0 && stack[stackTop] == OBJECT;
    }

    @Override
    public boolean inArray() {
        return stackTop >= 0 && stack[stackTop] == ARRAY;
    }

    public String getErrorString() {
        if (lastEvent != SerdeEvent.ERROR) {
            throw new IllegalStateException(
                "The last event was " + lastEvent + ", expected ERROR");
        }
        return lastError;
    }

    @Override
    public CharBuffer getCharBuffer() {
        if (lastEvent != SerdeEvent.VALUE_STRING &&
            lastEvent != SerdeEvent.OBJECT_KEY)
        {
            throw new IllegalStateException(
                "The last event was " + lastEvent + ", expected STRING");
        }
        return stringParser.getCharBuffer();
    }

    @Override
    public boolean getBoolean() {
        if (lastEvent != SerdeEvent.VALUE_BOOLEAN) {
            throw new IllegalStateException(
                "The last event was " + lastEvent + ", expected VALUE_BOOLEAN");
        }
        return booleanValue;
    }

    @Override
    public SerdeNumber getNumber() {
        if (lastEvent != SerdeEvent.VALUE_NUMBER) {
            throw new IllegalStateException(
                "The last event was " + lastEvent + ", expected VALUE_NUMBER");
        }
        return numberParser;
    }

    @Override
    protected byte[] readBytes(int start, int end) {
        byte[] result = new byte[end - start];
        ByteBuffer tmp = input.duplicate();
        tmp.position(start);
        tmp.get(result);
        return result;
    }

    @Override
    protected ByteBuffer readByteBuffer(int start, int end) {
        ByteBuffer tmp = input.duplicate();
        tmp.position(start);
        tmp.limit(end);
        return tmp;
    }

    @Override
    protected void handleUnexpectedChar() {
        lastError = String.format("Unexpected character 0x%02X", input.get(ts));
    }

    @Override
    protected boolean handleComma() {
        if (expect != Expect.COMMA) {
            lastError = "Expected " + expect + ", but got COMMA";
            return false;
        }
        expect = inObject() ? Expect.OBJECT_KEY : Expect.ANY;
        return true;
    }

    @Override
    protected boolean handleColon() {
        if (expect != Expect.COLON) {
            lastError = "Expected " + expect + ", but got COLON";
            return false;
        }
        expect = Expect.ANY;
        return true;
    }

    private SerdeEvent handle(SerdeEvent event) {
        switch (event) {
        case ERROR:         return event;
        case UNDERFLOW:     return handleUnderflow();
        case EOF:           return event;    
        case OBJECT_START:  return handleObjectStart();
        case OBJECT_END:    return handleObjectEnd();
        case ARRAY_START:   return handleArrayStart();
        case ARRAY_END:     return handleArrayEnd();
        case VALUE_NULL:    return handleValue(event);
        case VALUE_BOOLEAN: return handleValue(event);
        case VALUE_NUMBER:  return handleValue(event);
        case VALUE_STRING:  return handleString();
        case OBJECT_KEY:
            throw new IllegalStateException("Parser should not emit OBJECT_KEY!");
        }
        throw new IllegalStateException("Unknown event=" + event);
    }

    private SerdeEvent handleUnderflow() {
        if (te > 0) {
            // Update internal state and advance input position:
            input.position(te);
            baseOffset += te;
            mark -= te;
            pline -= te;
            cs -= te;
            p -= te;
            pe -= te;
            ts -= te;
            te = -1;
        }
        this.input = null;
        return SerdeEvent.UNDERFLOW;
    }

    private SerdeEvent handleObjectStart() {
        if (expect != Expect.ANY) {
            lastError = "Expected " + expect + ", but got OBJECT_START";
            return SerdeEvent.ERROR;
        }
        expect = Expect.OBJECT_KEY;
        return push(OBJECT) ? SerdeEvent.OBJECT_START : SerdeEvent.ERROR;
    }

    private SerdeEvent handleObjectEnd() {
        if (expect != Expect.OBJECT_KEY && expect != Expect.COMMA) {
            lastError = "Expected " + expect + ", but got OBJECT_END";
            return SerdeEvent.ERROR;
        }
        if (stackTop < 0) {
            lastError = "Unexpected OBJECT_END";
            return SerdeEvent.ERROR;
        }
        if (stack[stackTop--] != OBJECT) {
            lastError = "Expected ARRAY_END, but got OBJECT_END";
            return SerdeEvent.ERROR;
        }
        expect = stackTop < 0 ? Expect.DONE : Expect.COMMA;
        return SerdeEvent.OBJECT_END;
    }

    private SerdeEvent handleArrayStart() {
        if (expect != Expect.ANY) {
            lastError = "Expected " + expect + ", but got ARRAY_START";
            return SerdeEvent.ERROR;
        }
        expect = Expect.ANY;
        return push(ARRAY) ? SerdeEvent.ARRAY_START : SerdeEvent.ERROR;
    }

    private SerdeEvent handleArrayEnd() {
        if (expect != Expect.ANY && expect != Expect.COMMA) {
            lastError = "Expected " + expect + ", but got ARRAY_END";
            return SerdeEvent.ERROR;
        }
        if (stackTop < 0) {
            lastError = "Unexpected ARRAY_END";
            return SerdeEvent.ERROR;
        }
        if (stack[stackTop--] != OBJECT) {
            lastError = "Expected OBJECT_END, but got ARRAY_END";
            return SerdeEvent.ERROR;
        }
        expect = stackTop < 0 ? Expect.DONE : Expect.COMMA;
        return SerdeEvent.ARRAY_END;
    }

    private SerdeEvent handleString() {
        if (expect == Expect.OBJECT_KEY) {
            expect = Expect.COLON;
            return SerdeEvent.OBJECT_KEY;
        }
        if (expect != Expect.ANY) {
            lastError = "Expected " + expect + ", but got VALUE_NULL";
            return SerdeEvent.ERROR;
        }
        expect = stackTop < 0 ? Expect.DONE : Expect.COMMA;
        return SerdeEvent.VALUE_STRING;
    }

    private SerdeEvent handleValue(SerdeEvent event) {
        if (expect != Expect.ANY) {
            lastError = "Expected " + expect + ", but got " + event;
            return SerdeEvent.ERROR;
        }
        expect = stackTop < 0 ? Expect.DONE : Expect.COMMA;
        return event;
    }

    private boolean push(byte type) {
        if (stack.length - 1 == stackTop) {
            lastError = "JSON nesting exceeds limit of " + stack.length;
            return false;
        }
        stack[++stackTop] = type;
        return true;
    }
}
