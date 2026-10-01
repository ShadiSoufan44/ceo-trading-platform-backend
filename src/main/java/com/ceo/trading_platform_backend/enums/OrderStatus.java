package com.ceo.trading_platform_backend.enums;

public enum OrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    FUFILLED,
    CANCELLED;
}


// can orders go through multiple steps of status?