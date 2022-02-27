package com.brimworks.serde;

import java.nio.CharBuffer;

public interface SerdeReader {
    /**
     * Read the next "event" from the stream.
     * @return the next event.
     */
    SerdeEvent read();

    /**
     * The first line is 1.
     * @return the current line number of input.
     */
    int getLineNumber();

    /**
     * The first column in each line is 1.
     * @return the current column number of input.
     */
    int getColumnNumber();

    /**
     * The input offset. The first offset is 0.
     * @return the inputs offset.
     */
    long getOffset();

    /**
     * @return true if we are in the process of reading an object. Note that if
     *     the object contains an array value, then this will return false until
     *     the array value is completely read.
     */
    boolean inObject();

    /**
     * @return true if we are in the process of reading an array. Note that if
     *     the array contains an object element, then this will return false
     *     until the object element is completely read.
     */
    boolean inArray();

    /**
     * @return a description of the parse error.
     * @throws IllegalStateException unless the last SerdeEvent read() returned
     *     ERROR.
     */
    String getErrorString();

    /**
     * @return the char buffer that contains the object key or string value.
     *     This buffer is read-only and in "get" mode.
     * @throws IllegalStateException unless the last SerdeEvent read() returned
     *     either OBJECT_KEY or VALUE_STRING.
     */
    CharBuffer getCharBuffer();

    /**
     * @return the boolean value
     * @throws IllegalStateException unless the last SerdeEvent read() returned
     *     VALUE_BOOLEAN.
     */
    boolean getBoolean();

    /**
     * @return the number value
     * @throws IllegalStateException unless the last SerdeEvent read() returned
     *     VALUE_NUMBER.
     */
    SerdeNumber getNumber();
}
