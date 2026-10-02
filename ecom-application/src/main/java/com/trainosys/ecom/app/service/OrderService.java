package com.trainosys.ecom.app.service;

import com.trainosys.ecom.app.dto.OrderResponse;

import java.util.Optional;

public interface OrderService {
    Optional<OrderResponse> createOrder(String userId);
}
