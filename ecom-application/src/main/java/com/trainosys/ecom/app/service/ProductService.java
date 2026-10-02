package com.trainosys.ecom.app.service;

import com.trainosys.ecom.app.dto.ProductRequest;
import com.trainosys.ecom.app.dto.ProductResponse;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    ProductResponse createProduct(ProductRequest productRequest);
    Optional<ProductResponse> updateProduct(Long id, ProductRequest productRequest);
    List<ProductResponse> getAllProducts();
    boolean deleteProduct(Long id);
    List<ProductResponse> searchProducts(String keyword);
}
