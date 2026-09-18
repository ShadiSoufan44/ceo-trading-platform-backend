package com.ceo.trading_platform_backend.uml_objects;

public class User {
    
    public String name;
    public int userID;
    public String email;
    public String password;
    public String joinDate;



    public User(String name, int userID, String email, String password, String joinDate) {
        this.name = name;
        this.userID = userID;
        this.email = email;
        this.password = password;
        this.joinDate = joinDate;
    }

    

}
