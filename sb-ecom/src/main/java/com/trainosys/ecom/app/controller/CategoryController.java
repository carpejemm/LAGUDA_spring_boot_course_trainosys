package com.trainosys.ecom.app.controller;

import com.trainosys.ecom.app.model.Category;
import com.trainosys.ecom.app.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {


    // For now, we will use an in-memory list to store categories. In a real application, you would typically use a database for persistence.
//    private List<Category> categories = new ArrayList<>();

    private CategoryService categoryService;

    // Constructor injection for the CategoryService dependency
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/public/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        // Return the list of categories
        return new ResponseEntity<>(categoryService.getAllCategories(), HttpStatus.OK);
    }

    // This endpoint is for creating a new category. It accepts a Category object in the request body and adds it to the list of categories.
    // /admin ==> this is private endpoint, only admin can create new category. In a real application, you would typically implement authentication and authorization to restrict access to this endpoint.
    @PostMapping("/admin/categories")
    public ResponseEntity<String> createCategory(@RequestBody Category category) {
        categoryService.createCategory(category);
        return new ResponseEntity<>("Category created successfully!", HttpStatus.CREATED);
    }

    // This endpoint is for deleting a category. It accepts a Category object in the request body and removes it from the list of categories.
    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId) {
        try {
            String status = categoryService.deleteCategory(categoryId);
            return new ResponseEntity<>(status, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }

    // This endpoint is for updating a category. The new details come from the request body and the ID comes from the URL.
    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String> updateCategory(@RequestBody Category category,
                                                 @PathVariable Long categoryId) {
        try {
            Category savedCategory = categoryService.updateCategory(category, categoryId);
            return new ResponseEntity<>("Category with categoryId: " + categoryId + " updated successfully !!", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }
}
