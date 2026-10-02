package com.trainosys.ecom.app.controller;

import com.trainosys.ecom.app.dto.CartItemRequest;
import com.trainosys.ecom.app.model.CartItem;
import com.trainosys.ecom.app.security.services.UserDetailsImpl;
import com.trainosys.ecom.app.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<String> addToCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CartItemRequest request) {
        if (!cartService.addToCart(String.valueOf(userDetails.getId()), request)) {
            return ResponseEntity.badRequest().body("Product Out of Stock or User not found or Product not found");
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeFromCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long productId) {
        boolean deleted = cartService.deleteItemFromCart(String.valueOf(userDetails.getId()), productId);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(cartService.getCart(String.valueOf(userDetails.getId())));
    }

}
