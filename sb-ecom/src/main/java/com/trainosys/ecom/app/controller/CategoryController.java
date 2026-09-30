package com.trainosys.ecom.app.controller;

import com.trainosys.ecom.app.model.Category;
import com.trainosys.ecom.app.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoryController {


    // For now, we will use an in-memory list to store categories. In a real application, you would typically use a database for persistence.
//    private List<Category> categories = new ArrayList<>();

    private CategoryService categoryService;

    // Constructor injection for the CategoryService dependency
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/public/categories")
    public List<Category> getAllCategories() {
        // Return the list of categories
        return categoryService.getAllCategories();
    }

    // This endpoint is for creating a new category. It accepts a Category object in the request body and adds it to the list of categories.
    // /admin ==> this is private endpoint, only admin can create new category. In a real application, you would typically implement authentication and authorization to restrict access to this endpoint.
    @PostMapping("/api/admin/categories")
    public String createCategory(@RequestBody Category category) {
        categoryService.createCategory(category);
        return "Category created successfully!";
    }

    // This endpoint is for deleting a category. It accepts a Category object in the request body and removes it from the list of categories.
}
