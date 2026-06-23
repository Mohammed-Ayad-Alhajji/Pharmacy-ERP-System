// مسار الملف: src/main/java/com/pharmacy/services/interfaces/finance/ExpenseCategoryService.java

package com.pharmacy.services.interfaces.finance;

import com.pharmacy.models.finance.ExpenseCategory;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryService {

    Optional<ExpenseCategory> createCategory(ExpenseCategory category);

    boolean updateCategory(ExpenseCategory category);

    boolean deleteCategory(int categoryId);

    Optional<ExpenseCategory> getCategoryById(int id);

    Optional<ExpenseCategory> getCategoryByName(String name);

    List<ExpenseCategory> getAllCategories();
}