package com.brimworks.serde;

public interface SerdeWriter {
    default SerdeWriter writeObjectStart(Path path) {
        throw new TypeMismatchException("Unexpect object found", path);
    }

    default SerdeWriter writeObjectKey(Path path, String key) {
        throw new UnsupportedOperationException("Missing implementation of writeObjectKey()");
    }

    default SerdeWriter writeObjectEnd(Path path) {
        throw new UnsupportedOperationException("Missing implementation of writeObjectEnd()");
    }

    default SerdeWriter writeArrayStart(Path path) {
        throw new TypeMismatchException("Unexpected array found", path);
    }

    default SerdeWriter writeArrayEnd(Path path) {
        throw new UnsupportedOperationException("Missing implementation of writeArrayEnd()");
    }

    default SerdeWriter writeBoolean(Path path, boolean value) {
        throw new TypeMismatchException("Unexpected boolean found", path);
    }

    default SerdeWriter writeNull(Path path) {
        throw new TypeMismatchException("Unexpected null found", path);
    }

    default SerdeWriter writeString(Path path, String value) {
        throw new TypeMismatchException("Unexpected string found", path);
    }

    default SerdeWriter writeNumber(Path path, SerdeNumber value) {
        throw new TypeMismatchException("Unexpected number found", path);
    }
}
