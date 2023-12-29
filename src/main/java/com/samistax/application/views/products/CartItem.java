package com.samistax.application.views.products;

import java.util.Objects;

public class CartItem  {
    private String itemId = "";
    private String name = "";
    private String brand = "";
    private double price;
    private int qty = 0;

    public CartItem(String itemId, String name, String brand, String price) {
        this.itemId = itemId;
        this.name = name;
        this.brand = brand;
        try {
            this.price = Double.parseDouble(price);
        } catch ( NumberFormatException nfe){}
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Double.compare(cartItem.price, price) == 0 && qty == cartItem.qty && Objects.equals(itemId, cartItem.itemId) && Objects.equals(name, cartItem.name) && Objects.equals(brand, cartItem.brand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, name, brand, price, qty);
    }
}