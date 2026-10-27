package com.ceo.trading_platform_backend.models;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ceo.trading_platform_backend.enums.PortfolioType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity 
@Table(name = "portfolio")
public class Portfolio {

    public static final int NAME_MAX_LENGTH = 100;
    
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @NotBlank
    @Size(max = NAME_MAX_LENGTH)
    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "portfolio_id")
    private List<Holding> holdings = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private PortfolioType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    public Portfolio() {}

    public Portfolio(PortfolioType type) {
        this.type = type;
    }

    public Portfolio(PortfolioType type, String name) {
        this.type = type;
        this.name = name;
    }

    public List<Holding> getHoldings() {
        return this.holdings;
    }
    public PortfolioType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void addHolding(Holding holding) {
       this.holdings.add(holding);
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }
}
