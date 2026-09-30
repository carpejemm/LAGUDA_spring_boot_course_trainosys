package com.trainosys.ecom.app.service;

import com.trainosys.ecom.app.model.Category;
import java.util.List;

public interface CategoryService {
    // Method to retrieve all categories
    List<Category> getAllCategories();
    // Method to create a new category
    void createCategory(Category category);
}
