package com.brimworks.serde.json;

import com.brimworks.serde.Path;

public class JsonParseException extends RuntimeException {
    private Path path;

    public JsonParseException(String msg, Path path) {
        super(msg + "\n" + path);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
