package com.brimworks.serde.json;

import com.brimworks.serde.SerdePath;

public class JsonParseException extends RuntimeException {
    private SerdePath path;

    public JsonParseException(String msg, SerdePath path) {
        super(msg + "\n" + path);
        this.path = path;
    }

    public SerdePath getPath() {
        return path;
    }
}
