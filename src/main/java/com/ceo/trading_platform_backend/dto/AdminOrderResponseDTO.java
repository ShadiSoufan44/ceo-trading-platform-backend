package com.ceo.trading_platform_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;

/** Admin order view with client identity and settlement facts from linked holdings. */
public record AdminOrderResponseDTO(
        UUID orderId,
        UUID clientId,
        String clientName,
        UUID portfolioId,
        String addInstrumentSymbol,
        InstrumentType addInstrumentType,
        String addInstrumentFullName,
        String subtractInstrumentSymbol,
        InstrumentType subtractInstrumentType,
        String subtractInstrumentFullName,
        BigDecimal quantity,
        BigDecimal quotedPrice,
        BigDecimal addPurchasePrice,
        BigDecimal subtractPurchasePrice,
        BigDecimal unitsBought,
        BigDecimal unitsSold,
        BigDecimal cashBalanceDifference,
        Side side,
        BigDecimal changeThreshold,
        String createdDate,
        String resolvedDate,
        OrderStatus currentStatus
) {}