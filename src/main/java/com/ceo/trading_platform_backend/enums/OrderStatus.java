package com.ceo.trading_platform_backend.enums;

public enum OrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    FULFILLED,
    CANCELLED;
}


// can orders go through multiple steps of status?