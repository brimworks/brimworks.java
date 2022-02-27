package com.brimworks.serde.number;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.brimworks.serde.SerdeNumber;

public class SerdeDoubleNumber implements SerdeNumber {
    private double value;

    public SerdeDoubleNumber(double value) {
        this.value = value;
    }

    public SerdeDoubleNumber setValue(double value) {
        this.value = value;
        return this;
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(value);
    }

    @Override
    public BigInteger bigIntegerValue() {
        return bigDecimalValue().toBigInteger();
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return bigDecimalValue().toBigIntegerExact();
    }

    @Override
    public byte byteValue() {
        return (byte)value;
    }

    @Override
    public byte byteValueExact() {
        if (value < Byte.MIN_VALUE ||
            value > Byte.MAX_VALUE ||
            value % 1 != 0)
        {
            throw new ArithmeticException();
        }
        return byteValue();
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return (float)value;
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public int intValueExact() {
        if (value < Integer.MIN_VALUE ||
            value > Integer.MAX_VALUE ||
            value % 1 != 0)
        {
            throw new ArithmeticException();
        }
        return intValue();
    }
    
    @Override
    public long longValue() {
        return (long)value;
    }

    @Override
    public long longValueExact() {
        if (value % 1 != 0) {
            throw new ArithmeticException();
        }
        return longValue();
    }

    @Override
    public short shortValue() {
        return (short)value;
    }

    @Override
    public short shortValueExact() {
        if (value < Short.MIN_VALUE ||
            value > Short.MAX_VALUE ||
            value % 1 != 0)
        {
            throw new ArithmeticException();
        }
        return shortValue();
    }
   
}
