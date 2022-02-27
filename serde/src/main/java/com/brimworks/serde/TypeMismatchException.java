package com.brimworks.serde;

public class TypeMismatchException extends RuntimeException {
    private SerdePath path;

    public TypeMismatchException(String msg, SerdePath path) {
        super(msg + "\n" + path);
        this.path = path;
    }

    public SerdePath getPath() {
        return path;
    }
}
