package com.brimworks.serde;

public class TypeMismatchException extends RuntimeException {
    private Path path;

    public TypeMismatchException(String msg, Path path) {
        super(msg + "\n" + path);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
