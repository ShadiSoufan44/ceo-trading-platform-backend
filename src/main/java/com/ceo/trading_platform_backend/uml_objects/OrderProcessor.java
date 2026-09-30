package com.ceo.trading_platform_backend.uml_objects;

import java.util.List;

import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.uml_objects.Enums.Side;

public class OrderProcessor {
    

    //service object to communicate with API

    public static Order submit(Order order) throws Exception {
        
        if (order.getSide() == Side.BUY) {
            if (!hasSufficientFundsBuy(order)) {
                
                order.setMessage("Insufficient Funds");
                return order;
            }
        } else if (order.getSide() == Side.SELL) {
            if (!hasSufficientHoldingsSell(order)) {
                order.setMessage("Insufficient Holdings");
                return order;
            }
        }

        if (!instrumentIsTradeable(order)) {
            order.setMessage("Instrument Not Tradeable");
        }
        

        return execute(order);
    }
    
    // Buy Order
    private static boolean hasSufficientFundsBuy(Order order) throws Exception {
        double funds = order.getPortfolio().getUSDCash();
        double cost = order.getQuantity() * order.getQuotedPrice();

        return funds >= cost;
    }

    // Sell Order

    // 
    private static boolean hasSufficientHoldingsSell(Order order) {
        List<Holding> holdings = order.getPortfolio().getHoldings();

        double numSharesOwned = 0;

        for (Holding holding : holdings) {
            if (holding.getInstrument().equals(order.getInstrument())) {
                numSharesOwned += holding.getQuantity();
            }
        }

        if (numSharesOwned < order.getQuantity()) {
            return false;
        }
        
        return true;
    }

    private static boolean instrumentIsTradeable(Order order) {
        // comes from market API
        return true;
    }



    private static Order execute(Order order) {
        // need idempotency here somehow

        Client client = order.getClient();

        

        return order;
    }
}
