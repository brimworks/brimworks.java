package com.brimworks.serde;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface SerdeNumber {
    /**
     * May be implemented (although less efficiently by overriding this one method)
     */
    BigDecimal bigDecimalValue();

    default BigInteger bigIntegerValue() {
        return bigDecimalValue().toBigInteger();
    }

    default BigInteger bigIntegerValueExact() {
        return bigDecimalValue().toBigIntegerExact();
    }

    default byte byteValue() {
        return bigDecimalValue().byteValue();
    }

    default byte byteValueExact() {
        return bigDecimalValue().byteValueExact();
    }

    default double doubleValue() {
        return bigDecimalValue().doubleValue();
    }

    default float floatValue() {
        return bigDecimalValue().floatValue();
    }

    default int intValue() {
        return bigDecimalValue().intValue();
    }

    default int intValueExact() {
        return bigDecimalValue().intValueExact();
    }
    
    default long longValue() {
        return bigDecimalValue().longValue();
    }

    default long longValueExact() {
        return bigDecimalValue().longValueExact();
    }

    default short shortValue() {
        return bigDecimalValue().shortValue();
    }

    default short shortValueExact() {
        return bigDecimalValue().shortValueExact();
    }
}
