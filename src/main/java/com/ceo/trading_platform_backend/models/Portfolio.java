package com.ceo.trading_platform_backend.models;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ceo.trading_platform_backend.uml_objects.Instrument;
import com.ceo.trading_platform_backend.uml_objects.Enums.InstrumentType;
import com.ceo.trading_platform_backend.uml_objects.Enums.PortfolioType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity 
@Table(name = "portfolio")
public class Portfolio {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ID;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "portfolio_id")
    private List<Holding> holdings = new ArrayList<>();

    private PortfolioType type;

    public Portfolio(PortfolioType type, double cashBalance) {
        
        // Make Instrument Registry?
        this.holdings.add(new Holding(
            new Date(),
            1.0, 
            cashBalance, 
            new Instrument(
                "USD", InstrumentType.CASH, "United States Dollar"
            ), 
            null
        ));
        // TODO: re-use the Instrument from the instrument table
        this.type = type;
    }

    public List<Holding> getHoldings() {
        return this.holdings;
    }
    public PortfolioType getType() {
        return type;
    }

    public double getPortfolioBuyingPower() {
        double totalValue = 0;
        for (Holding holding : holdings) {
            if (holding.getInstrument().getSymbol() == "USD") {
                totalValue += holding.getQuantity();
            }
        }
        return totalValue;
    }

    public void addHolding(Holding holding) {
       this.holdings.add(holding);
    }

    public double getPortfolioTotalValue() {
        double value = getPortfolioBuyingPower();

        for (Holding holding : holdings) {
            double price = service.getPrice(holding.getInstrument());
            value += (price * holding.getQuantity());
        }
        return value;
    }

    public int getID() {
        return ID;
    }
}
