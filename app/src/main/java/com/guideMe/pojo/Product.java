package com.guideMe.pojo;

public class Product {
    public String id;
    public String image;
    public String name;
    public String category;
    public Double price;
    public Integer quantity;
    public String description;
    //
    public Integer requestedQuantity;
    public Product() {
        requestedQuantity=0;
    }

    public Product(String id, Integer requestedQuantity) {
        this.id = id;
        this.requestedQuantity = requestedQuantity;
    }
}
