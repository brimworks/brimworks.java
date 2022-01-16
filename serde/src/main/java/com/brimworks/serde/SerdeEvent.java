package com.brimworks.serde;

public enum SerdeEvent {
    ERROR,
    UNDERFLOW,
    EOF,

    OBJECT_START,
    OBJECT_KEY,
    OBJECT_END,

    ARRAY_START,
    ARRAY_END,

    VALUE_NULL,
    VALUE_BOOLEAN,
    VALUE_NUMBER,
    VALUE_STRING;
}
