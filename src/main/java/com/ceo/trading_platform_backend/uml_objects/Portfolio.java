package com.ceo.trading_platform_backend.uml_objects;
import java.util.ArrayList;
import java.util.List;

import com.ceo.trading_platform_backend.uml_objects.Enums.PortfolioType;

public class Portfolio {
    
    List<Holding> holdings = new ArrayList<>();
    PortfolioType type;
    String ID;
    double cashBalance;

    public Portfolio(PortfolioType type, double cashBalance) {
        
        this.type = type;
        this.cashBalance = cashBalance;
        //this.ID = generateUID();   figure out what would generate it
    }

    List<Holding> getHoldings() {
        return this.holdings;
    }

    double getUSDCash() {
        return this.cashBalance;
    }

    void addStockHolding(Holding holding) {
       this.holdings.add(holding);
    }

    double getPortfolioValue() {
        double value = this.cashBalance;

        for (Holding holding: holdings) {
            value += (holding.getInstrument().getPrice() * holding.getQuantity()) ;
        }
        return value;
    }
}
