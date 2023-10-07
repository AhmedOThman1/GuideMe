package com.guideMe.pojo;

import java.util.ArrayList;

public class Helper {
    public String id;
    public String photo;
    public String name;
    public String phone;
    public String email;
    public String token;
    public PaymentCardInfo paymentCardInfo;

    public ArrayList<Product> cartItems;

    public Helper() {
        cartItems = new ArrayList<>();
    }


}
