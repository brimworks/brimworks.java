package com.brimworks.serde.json;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

import com.brimworks.serde.SerdeEvent;
import com.brimworks.serde.SerdeNumber;
import com.brimworks.serde.SerdeWriter;

import org.junit.jupiter.api.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class JsonByteParserTest {
    private JsonByteReader reader = new JsonByteReader();

    @Test
    public void parseSimpleString() {
        assertEquals("hello\tworld", parseString("\"hello\\tworld\""));
    }

    @Test
    public void parseUnicode() {
        assertEquals("heart/❤❤/world", parseString("\"heart\\/\u2764❤/world\""));
    }

    @Test
    public void parseNumber() {
        assertEquals("12.345e67", parseNumber("+12.345E+67"));
    }

    @Test
    public void testIllegalChar() {
        feed("{\n\t\"a\": 1,\n\t\"b\": [A]}", true);
        assertEquals(readNonError(), SerdeEvent.OBJECT_START);
        assertEquals(readNonError(), SerdeEvent.OBJECT_KEY);
        assertEquals(reader.getCharBuffer().toString(), "a");
        assertEquals(readNonError(), SerdeEvent.VALUE_NUMBER);
        assertEquals(reader.getNumber().toString(), "1");
        assertEquals(readNonError(), SerdeEvent.OBJECT_KEY);
        assertEquals(reader.getCharBuffer().toString(), "b");
        assertEquals(readNonError(), SerdeEvent.ARRAY_START);
        assertEquals(reader.read(), SerdeEvent.ERROR);
        assertEquals(reader.getErrorString(), "Unexpected character 0x41");
        assertEquals(reader.getLineNumber(), 3);
        assertEquals(reader.getColumnNumber(), 8);
        assertEquals(reader.getOffset(), 18);
    }

    private SerdeEvent readNonError() {
        SerdeEvent result = reader.read();
        if (SerdeEvent.ERROR == result) {
            throw new RuntimeException("parse error: " + reader.getErrorString());
        }
        return result;
    }

    private void feed(String json, boolean done) {
        reader.feed(ByteBuffer.wrap(json.getBytes()), done);
    }

    private String parseString(String json) {
        feed(json, true);
        assertEquals(readNonError(), SerdeEvent.VALUE_STRING);
        return reader.getCharBuffer().toString();
    }

    private String parseNumber(String json) {
        feed(json, true);
        assertEquals(readNonError(), SerdeEvent.VALUE_NUMBER);
        return reader.getNumber().toString();
    }
}
