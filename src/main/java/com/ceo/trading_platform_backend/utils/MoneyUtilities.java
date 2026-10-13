package com.ceo.trading_platform_backend.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtilities {
    
    private static final int MONEY_SCALE = 8;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_EVEN;

    public static BigDecimal safeDivide(BigDecimal a, BigDecimal b) {
        return a.divide(b, MONEY_SCALE, MONEY_ROUNDING);
    }
}
