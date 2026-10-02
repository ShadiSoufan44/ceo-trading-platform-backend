package com.ceo.trading_platform_backend.models;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.PortfolioType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
    
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "portfolio_id")
    private List<Holding> holdings = new ArrayList<>();

    private PortfolioType type;

    public Portfolio(PortfolioType type) {
        this.type = type;
    }

    public List<Holding> getHoldings() {
        return this.holdings;
    }
    public PortfolioType getType() {
        return type;
    }

    public void addHolding(Holding holding) {
       this.holdings.add(holding);
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }
}
