package com.trainosys.ecom.app.service;

import com.trainosys.ecom.app.model.Category;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// This annotation indicates that this class is a Spring service component, which is a specialized type of @Component. It is used to define business logic and can be automatically detected and registered as a Spring bean during component scanning.
@Service
public final class CategoryServiceImpl implements CategoryService {
    private List<Category> categories = new ArrayList<>();
    private Long nextId = 1L; // Variable to keep track of the next category ID

    // Override the getAllCategories method to return the list of categories
    @Override
    public List<Category> getAllCategories() {
        return categories;
    }

    // Override the createCategory method to add a new category to the list
    @Override
    public void createCategory(Category category) {
        category.setCategoryId(nextId++);
        categories.add(category);
    }
}
