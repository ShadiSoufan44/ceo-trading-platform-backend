package com.ceo.trading_platform_backend.uml_objects;

import java.util.Set;
import java.util.HashSet;

public class Client extends User{
    
    Set<Portfolio> portfolios;

    public Client(String name, int userID, String email, String password, String joinDate) {
        super(name, userID, email, password, joinDate);
        portfolios = new HashSet<>();
    }

    String getName() {
        return this.name;
    }

}
 