// مسار الملف: src/main/java/com/pharmacy/services/impl/finance/ExpenseCategoryServiceImpl.java

package com.pharmacy.services.impl.finance;

import com.pharmacy.dao.interfaces.finance.ExpenseCategoryDAO;
import com.pharmacy.models.finance.ExpenseCategory;
import com.pharmacy.services.interfaces.finance.ExpenseCategoryService;

import java.util.List;
import java.util.Optional;

public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryDAO expenseCategoryDAO;

    public ExpenseCategoryServiceImpl(ExpenseCategoryDAO expenseCategoryDAO) {
        this.expenseCategoryDAO = expenseCategoryDAO;
    }

    @Override
    public Optional<ExpenseCategory> createCategory(ExpenseCategory category) {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم التصنيف مطلوب");
        }

        if (expenseCategoryDAO.findByName(category.getName()).isPresent()) {
            throw new IllegalArgumentException("هذا التصنيف موجود مسبقاً");
        }

        return expenseCategoryDAO.create(category);
    }

    @Override
    public boolean updateCategory(ExpenseCategory category) {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم التصنيف مطلوب");
        }

        Optional<ExpenseCategory> existingCategory = expenseCategoryDAO.findByName(category.getName());
        if (existingCategory.isPresent() && existingCategory.get().getCategory_id() != category.getCategory_id()) {
            throw new IllegalArgumentException("هذا التصنيف موجود مسبقاً");
        }

        return expenseCategoryDAO.update(category);
    }

    @Override
    public boolean deleteCategory(int categoryId) {
        try {
            boolean isDeleted = expenseCategoryDAO.delete(categoryId);
            if (!isDeleted) {
                throw new IllegalStateException("لا يمكن حذف هذا التصنيف لارتباطه بمصروفات سابقة");
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("لا يمكن حذف هذا التصنيف لارتباطه بمصروفات سابقة", e);
        }
    }

    @Override
    public Optional<ExpenseCategory> getCategoryById(int id) {
        return expenseCategoryDAO.findById(id);
    }

    @Override
    public Optional<ExpenseCategory> getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return expenseCategoryDAO.findByName(name);
    }

    @Override
    public List<ExpenseCategory> getAllCategories() {
        return expenseCategoryDAO.findAll();
    }
}