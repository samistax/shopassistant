package com.samistax.application.views.products;

import com.samistax.application.data.astra.json.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Cart {

    private HashMap<String, List<CartItem>> carts = new HashMap<>();

    public Cart() {}

    public List<CartItem> getCart(String userId) {
        if (! carts.containsKey(userId)) {
            carts.put(userId, new ArrayList<CartItem>() );
        }
        // get User cart
        return carts.get(userId);
    }
    public List<CartItem> addItem(String userId, Product product) {
        // Get user cart
        List<CartItem> cart = getCart(userId);
        cart.add(new CartItem(product.getPid(), product.getName(), product.getBrand(), product.getWholesale_price()));
        //carts.put(userId, cart );
        // get User cart
        return cart;
    }
    public boolean removeItem(String userId, CartItem item) {
        // Get user cart
        List<CartItem> cart = getCart(userId);
        return cart.remove(item);
    }

}
