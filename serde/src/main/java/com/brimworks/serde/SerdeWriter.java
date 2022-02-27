package com.brimworks.serde;

/**
 * All methods return false if the write buffer is full (aka buffer overflow).
 */
public interface SerdeWriter {
    boolean writeObjectStart();
    boolean writeObjectKey(CharSequence key);
    boolean writeObjectEnd();

    boolean writeArrayStart();
    boolean writeArrayEnd();

    boolean writeNull();
    boolean write(boolean value);
    boolean write(SerdeNumber value);
    boolean write(CharSequence value);
}
