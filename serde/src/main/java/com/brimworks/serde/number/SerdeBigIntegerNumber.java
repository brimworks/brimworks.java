package com.brimworks.serde.number;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.brimworks.serde.SerdeNumber;

public class SerdeBigIntegerNumber implements SerdeNumber {
    private BigInteger value;

    public SerdeBigIntegerNumber(BigInteger value) {
        this.value = value;
    }

    public SerdeBigIntegerNumber setValue(BigInteger value) {
        this.value = value;
        return this;
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(value);
    }

    @Override
    public BigInteger bigIntegerValue() {
        return value;
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return value;
    }

    @Override
    public byte byteValue() {
        return value.byteValue();
    }

    @Override
    public byte byteValueExact() {
        return value.byteValueExact();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public int intValueExact() {
        return value.intValueExact();
    }
    
    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public long longValueExact() {
        return value.longValueExact();
    }

    @Override
    public short shortValue() {
        return value.shortValue();
    }

    @Override
    public short shortValueExact() {
        return value.shortValueExact();
    }
   
}
