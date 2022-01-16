package com.brimworks.serde;

public interface SerdeBuilder<T> extends SerdeWriter {
    T build();
}
