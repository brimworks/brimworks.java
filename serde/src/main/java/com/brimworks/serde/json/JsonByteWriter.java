package com.brimworks.serde.json;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.brimworks.serde.SerdeNumber;
import com.brimworks.serde.SerdePath;
import com.brimworks.serde.SerdeWriter;
import com.brimworks.serde.TypeMismatchException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JsonByteWriter implements SerdeWriter {
    private static final byte OBJECT = 0x1;
    private static final byte ARRAY  = 0x2;
    // x & 4 == Stringy
    private static final byte STRING = 0x4;
    private static final byte OBJECT_KEY = 0xC;

    private static final byte SYMBOL = 0x8;
    private static final CharBuffer NULL_BUFF = CharBuffer.wrap("null");
    private static final CharBuffer TRUE_BUFF = CharBuffer.wrap("true");
    private static final CharBuffer FALSE_BUFF = CharBuffer.wrap("false");

    private CharsetEncoder charsetEncoder = UTF_8.newEncoder();

    private ByteBuffer output;
    private byte[] stack = new byte[2048];
    private int stackTop = -1;

    private CharBuffer currentValue = null;
    private boolean inEscape = false;
    private int currentValueOffset = 0;

    /**
     * Feed the next ByteBuffer to write into.
     * @param output in "put" mode from which to write into.
     * @return true if output buffer was immediately filled and thus
     *     another call to feed needs to be made.
     */
    public boolean feed(ByteBuffer output) {
        assert null != output && output.remaining() > 0;
        this.output = output;
        return !writeCurrentValue();
    }

    private boolean writeCurrentValue() {
        if (stackTop < 0) {
            return true;
        }
        if (4 == (stack[stackTop] & 4)) {  // Stringy
            // Write begin string:
            if (currentValueOffset < 0) {
                currentValueOffset = 0;
                output.put((byte)'"');
            }
            // Write UTF_8 encoded and escaped (", /, \) value:
            while (output.hasRemaining()) {
                int limit = currentValue.limit();
                boolean endOfInput = true;
                int position = currentValue.position();
                int max = position + output.remaining();
                if (limit < max) {
                    limit = max;
                }
            FIND_SPECIAL_CHAR:
                for (; position < max; position++) {
                    switch (currentValue.get(position)) {
                    case '"':
                    case '/':
                    case '\\':
                        currentValue.limit(position);
                        endOfInput = false;
                        break FIND_SPECIAL_CHAR;
                    }
                }
                currentValue.limit(max);
                CoderResult result = charsetEncoder.encode(
                    currentValue, output, endOfInput);
                if (CoderResult.OVERFLOW == result) {
                    return false;
                }
                if (CoderResult.UNDERFLOW != result) {
                    throw new IllegalStateException(
                        "Unexpected coder result " + result);
                }
                if (currentValue.hasRemaining()) {
                    throw new IllegalStateException(
                        "Unexpected remainder when encoding and receiving UNDERFLOW");
                }
                if (limit != max) {
                    // Escaped special char.
                    output.put((byte)'\\');
                    // FIXME: What if output has no remaining!
                    output.put((byte)currentValue.get());
                }
            }

            if (stack[stackTop] != OBJECT_KEY) {
                stackTop--;
            }
        } else { // Symbol
        }
        return true;
    }

    private void preValue() {
        // Valid transition states:
        // from ARRAY, from OBJECT_KEY
        if (stackTop > 0 && 2 != (stack[stackTop] & 2)) {
            throw new IllegalStateException(
                "Attempt to write a value, when not in an array or object value");
        }
        if (stack[stackTop] == OBJECT_KEY) {
            // Pop the stack:
            stackTop--;
        }
    }

    @Override
    public boolean writeObjectStart() {
        if (!output.hasRemaining()) return false;
        preValue();
        output.put((byte)'{');
        stack[++stackTop] = OBJECT;
        return true;
    }

    @Override
    public boolean writeObjectKey(CharSequence key) {
        assert null != key;
        if (!output.hasRemaining()) return false;
        if (stackTop < 0 || stack[stackTop] != OBJECT) {
            throw new IllegalStateException(
                "Attempt to writeObjectKey() when not in an object");
        }
        stack[++stackTop] = OBJECT_KEY;
        inEscape = false;
        currentValue = CharBuffer.wrap(key);
        currentValueOffset = -1;
        return writeCurrentValue();
    }

    @Override
    public boolean writeObjectEnd() {
        if (!output.hasRemaining()) return false;
        if (stackTop < 0 || stack[stackTop] != OBJECT) {
            throw new IllegalStateException(
                "Attempt to writeObjectEnd() when not in an object");
        }
        output.put((byte)'}');
        return true;
    }

    @Override
    public boolean writeArrayStart() {
        if (!output.hasRemaining()) return false;
        preValue();
        output.put((byte)'[');
        stack[++stackTop] = ARRAY;
        return true;
    }

    @Override
    public boolean writeArrayEnd() {
        if (!output.hasRemaining()) return false;
        if (stackTop < 0 || stack[stackTop] != ARRAY) {
            throw new IllegalStateException(
                "Attempt to writeArrayEnd() when not in an array");
        }
        output.put((byte)']');
        return true;
    }

    @Override
    public boolean writeNull() {
        stack[++stackTop] = SYMBOL;
        inEscape = false;
        currentValue = NULL_BUFF.asReadOnlyBuffer();
        currentValueOffset = -1;
        return writeCurrentValue();
    }

    @Override
    public boolean write(boolean value) {
        stack[++stackTop] = SYMBOL;
        inEscape = false;
        currentValue = ( value ? TRUE_BUFF : FALSE_BUFF).asReadOnlyBuffer();
        currentValueOffset = -1;
        return writeCurrentValue();
    }

    @Override
    public boolean write(SerdeNumber value) {
        stack[++stackTop] = SYMBOL;
        inEscape = false;
        // TODO: Make this more efficient (perhaps add method to SerdeNumber
        // that returns the byte[]s?)
        currentValue = CharBuffer.wrap(value.toString());
        currentValueOffset = -1;
        return writeCurrentValue();
    }

    @Override
    public boolean write(CharSequence value) {
        stack[++stackTop] = STRING;
        inEscape = false;
        currentValue = CharBuffer.wrap(value);
        currentValueOffset = -1;
        return writeCurrentValue();
    }

}
