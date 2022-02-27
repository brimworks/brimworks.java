package com.brimworks.nio;

import java.io.IOException;

@FunctionalInterface
public interface Callback<T> {
    void accept(T arg) throws IOException;
}
