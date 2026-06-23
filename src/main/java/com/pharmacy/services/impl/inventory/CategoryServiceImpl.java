// مسار الملف: src/main/java/com/pharmacy/services/impl/inventory/CategoryServiceImpl.java

package com.pharmacy.services.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.CategoryDAO;
import com.pharmacy.models.inventory.Category;
import com.pharmacy.services.interfaces.inventory.CategoryService;

import java.util.List;
import java.util.Optional;

public class CategoryServiceImpl implements CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryServiceImpl(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    @Override
    public Optional<Category> createCategory(Category category) {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم التصنيف مطلوب");
        }

        if (categoryDAO.findByName(category.getName()).isPresent()) {
            throw new IllegalArgumentException("اسم التصنيف موجود مسبقاً في النظام");
        }

        return categoryDAO.create(category);
    }

    @Override
    public Optional<Category> getCategoryById(int id) {
        return categoryDAO.findById(id);
    }

    @Override
    public Optional<Category> getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return categoryDAO.findByName(name);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    @Override
    public boolean updateCategory(Category category) {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم التصنيف مطلوب");
        }

        Optional<Category> existingCategory = categoryDAO.findByName(category.getName());
        if (existingCategory.isPresent() && existingCategory.get().getCategory_id()!= category.getCategory_id()) {
            throw new IllegalArgumentException("يوجد تصنيف آخر يحمل نفس هذا الاسم");
        }

        return categoryDAO.update(category);
    }

    @Override
    public boolean deleteCategory(int id) {
        try {
            boolean isDeleted = categoryDAO.delete(id);
            if (!isDeleted) {
                // قد ترجع الـ DAO قيمة false عند الفشل دون رمي استثناء في بعض التطبيقات، لذا نعالجها هنا أيضاً.
                throw new IllegalStateException("لا يمكن حذف هذا التصنيف لوجود أدوية مرتبطة به في المستودع");
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("لا يمكن حذف هذا التصنيف لوجود أدوية مرتبطة به في المستودع", e);
        }
    }
}