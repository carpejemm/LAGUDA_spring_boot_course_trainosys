package com.trainosys.ecom.app.service;

import com.trainosys.ecom.app.dto.CartItemRequest;
import com.trainosys.ecom.app.model.CartItem;

import java.util.List;

public interface CartService {
    boolean addToCart(String userId, CartItemRequest request);
    boolean deleteItemFromCart(String userId, Long productId);
    List<CartItem> getCart(String userId);
    void clearCart(String userId);
}
