package com.ceo.trading_platform_backend.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.ceo.trading_platform_backend.dto.QuoteSubscriptionRequest;
import com.ceo.trading_platform_backend.services.QuoteStreamingService;

@Controller
public class RealtimeQuoteController {

    private final QuoteStreamingService quoteStreamingService;

    public RealtimeQuoteController(QuoteStreamingService quoteStreamingService) {
        this.quoteStreamingService = quoteStreamingService;
    }

    @MessageMapping("/quotes/subscribe")
    public void subscribe(QuoteSubscriptionRequest request) {
        quoteStreamingService.trackSymbol(request.symbol(), request.type());
    }
}
