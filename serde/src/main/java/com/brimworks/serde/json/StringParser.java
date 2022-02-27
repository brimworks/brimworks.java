package com.brimworks.serde.json;

import java.nio.CharBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class StringParser {
    private CharsetDecoder charsetDecoder = UTF_8.newDecoder();
    private CharBuffer charBuffer = null;

    public void reset() {
        charsetDecoder.reset();
        if (null != charBuffer) charBuffer.clear();
    }

    public void appendChar(char ch) {
        charBuffer = realloc(charBuffer, 1);
        charBuffer.put(ch);
    }

    public void appendUnicodeChar(byte[] base16) {
        assert base16 != null && base16.length == 4;
        appendChar((char)
            ((((base16[0] & 0xF) + (base16[0] > '9' ? 9 : 0)) << 12) &
             (((base16[1] & 0xF) + (base16[1] > '9' ? 9 : 0)) << 12) &
             (((base16[2] & 0xF) + (base16[2] > '9' ? 9 : 0)) << 12) &
             (((base16[3] & 0xF) + (base16[3] > '9' ? 9 : 0)) << 12)));
    }

    protected CharBuffer realloc(CharBuffer current, int minRemaining) {
        if (null == current) {
            int capacity = 8 * 1024;
            if (minRemaining > capacity) {
                capacity = minRemaining;
            }
            return CharBuffer.allocate(capacity);
        }
        int remaining = current.remaining();
        if (remaining >= minRemaining) {
            return current;
        }
        int capacity = 2 * current.capacity();
        if (minRemaining > capacity) {
            capacity = minRemaining;
        }
        CharBuffer result = CharBuffer.allocate(capacity);
        current.flip();
        result.put(current);
        return result;
    }

    public void append(ByteBuffer byteBuffer) {
        charBuffer = realloc(charBuffer, byteBuffer.remaining());
        while (
            CoderResult.OVERFLOW ==
            charsetDecoder.decode(byteBuffer, charBuffer, true))
        {
            charBuffer = realloc(charBuffer, charBuffer.capacity());
        }
    }

    public CharBuffer getCharBuffer() {
        CharBuffer copy = charBuffer.asReadOnlyBuffer();
        copy.flip();
        return copy;
    }

    public String toString() {
        return getCharBuffer().toString();
    }
}
