package com.brimworks.serde.json;

import java.math.BigDecimal;

import com.brimworks.serde.SerdeNumber;

public class NumberParser implements SerdeNumber {
    private int sign = 1;
    private byte[] integer;
    private byte[] fraction;
    private int exponentSign = 1;
    private byte[] exponent;

    public void reset() {
        sign = 1;
        integer = null;
        fraction = null;
        exponentSign = 1;
        exponent = null;
    }

    public void negate() {
        sign = -1;
    }

    public void integer(byte[] base10) {
        this.integer = base10;
    }

    public void fraction(byte[] base10) {
        this.fraction = base10;
    }

    public void negateExponent() {
        this.exponentSign = -1;
    }

    public void exponent(byte[] base10) {
        this.exponent = base10;
    }

    private int toStringLength() {
        int len = integer.length;
        if (sign < 0) len++;
        if (null != fraction) {
            len += fraction.length + 1;
        }
        if (null != exponent) {
            if (exponentSign < 0) len++;
            len += exponent.length + 1;
        }
        return len;
    }

    @Override
    public String toString() {
        char[] result = new char[toStringLength()];
        if (result.length > 0) {
            int idx = 0;
            if (sign < 0) {
                result[idx++] = '-';
            }
            for (int i = 0; i < integer.length; i++) {
                result[idx++] = (char)integer[i];
            }
            if (null != fraction) {
                result[idx++] = '.';
                for (int i = 0; i < fraction.length; i++) {
                    result[idx++] = (char)fraction[i];
                }
            }
            if (null != exponent) {
                result[idx++] = 'e';
                if (exponentSign < 0) {
                    result[idx++] = '-';
                }
                for (int i = 0; i < exponent.length; i++) {
                    result[idx++] = (char)exponent[i];
                }
            }    
        }
        return new String(result);
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(toString());
    }

    // TODO: Make this more efficient by implementing the other methods!
}
