// مسار الملف: src/main/java/com/pharmacy/services/interfaces/inventory/CategoryService.java

package com.pharmacy.services.interfaces.inventory;

import com.pharmacy.models.inventory.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    Optional<Category> createCategory(Category category);

    Optional<Category> getCategoryById(int id);

    Optional<Category> getCategoryByName(String name);

    List<Category> getAllCategories();

    boolean updateCategory(Category category);

    boolean deleteCategory(int id);
}