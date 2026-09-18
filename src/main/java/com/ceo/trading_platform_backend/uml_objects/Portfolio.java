package com.ceo.trading_platform_backend.uml_objects;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ceo.trading_platform_backend.uml_objects.Enums.InstrumentType;
import com.ceo.trading_platform_backend.uml_objects.Enums.PortfolioType;

public class Portfolio {
    
    List<Holding> holdings = new ArrayList<>();
    PortfolioType type;
    int ID;

    public Portfolio(PortfolioType type, double cashBalance) {
        
        // Make Instrument Registry?
        this.holdings.add(new Holding(
            new Date(), 1.0, cashBalance, 
            new Instrument("USD", InstrumentType.CASH)
        ));
        // TODO: re-use the Instrument from the instrument table
        this.type = type;
        //this.ID = generateUID();   figure out what would generate it
    }

    List<Holding> getHoldings() {
        return this.holdings;
    }

    double getUSDCash() throws Exception {
        for (Holding holding : holdings) {
            if (holding.instrument.symbol == "USD") {
                return holding.getQuantity();
            }
        }
        throw new Exception("There is no USD in this portfolio");
    }

    void addStockHolding(Holding holding) {
       this.holdings.add(holding);
    }

    double getPortfolioValue() throws Exception {
        double value = getUSDCash();

        for (Holding holding : holdings) {
            double price = service.getPrice(holding.getInstrument());
            value += (price * holding.getQuantity());
        }
        return value;
    }
}
