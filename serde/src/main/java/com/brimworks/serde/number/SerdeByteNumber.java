package com.brimworks.serde.number;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.brimworks.serde.SerdeNumber;

public class SerdeByteNumber implements SerdeNumber {
    private byte value;

    public SerdeByteNumber(byte value) {
        this.value = value;
    }

    public SerdeByteNumber setValue(byte value) {
        this.value = value;
        return this;
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(value);
    }

    @Override
    public BigInteger bigIntegerValue() {
        return BigInteger.valueOf(value);
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return BigInteger.valueOf(value);
    }

    @Override
    public byte byteValue() {
        return value;
    }

    @Override
    public byte byteValueExact() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public int intValueExact() {
        return value;
    }
    
    @Override
    public long longValue() {
        return value;
    }

    @Override
    public long longValueExact() {
        return value;
    }

    @Override
    public short shortValue() {
        return value;
    }

    @Override
    public short shortValueExact() {
        return value;
    }
   
}
