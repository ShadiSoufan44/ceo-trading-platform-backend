package com.ceo.trading_platform_backend.uml_objects;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.HashSet;

/**
 * Represents a Client user - self-directed investor with personal portfolio(s).
 * Persona: Joanna - Trades a personal portfolio, wants quick execution (< 1 min),
 * real-time position visibility, no jargon or complex workflows.
 */
public class Client extends User {
    
    private Set<Portfolio> portfolios;

    public Client() {
        super();
        portfolios = new HashSet<>();
    }

    public Client(String fullName, String email, String password, OffsetDateTime joinDate) {
        super(fullName, email, password, joinDate, "CLIENT");
        portfolios = new HashSet<>();
    }

    public Set<Portfolio> getPortfolios() {
        return portfolios;
    }

    public Portfolio getPortfolio(int portfolioID) {
        for (Portfolio portfolio : portfolios) {
            if (portfolio.ID == portfolioID) {
                return portfolio;
            }
        }
        return null;
    }

    // TODO: Client-specific capabilities
    // - placeOrder(Order order) - Execute trade in < 1 minute
    // - getPositions() - Real-time position visibility
    // - viewTransactionHistory() - Order history and fills
    // - canViewUser(Integer userId) - Only own data, false for others
}
 