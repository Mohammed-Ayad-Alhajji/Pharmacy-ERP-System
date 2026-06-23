// مسار الملف: src/main/java/com/pharmacy/services/impl/inventory/InventoryAdjustmentServiceImpl.java

package com.pharmacy.services.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.InventoryAdjustmentDAO;
import com.pharmacy.models.inventory.InventoryAdjustment;
import com.pharmacy.services.interfaces.inventory.InventoryAdjustmentService;
import com.pharmacy.security.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * تنفيذ خدمة تسوية المخزون.
 * يتم التركيز هنا على دقة الحسابات الفنية وضمان تسجيل المستخدم القائم بالعملية.
 */
public class InventoryAdjustmentServiceImpl implements InventoryAdjustmentService {

    private final InventoryAdjustmentDAO inventoryAdjustmentDAO;

    public InventoryAdjustmentServiceImpl(InventoryAdjustmentDAO inventoryAdjustmentDAO) {
        this.inventoryAdjustmentDAO = inventoryAdjustmentDAO;
    }

    @Override
    public Optional<InventoryAdjustment> createAdjustment(InventoryAdjustment adjustment) {
        // 1. التحقق من صحة البيانات الأساسية
        if (adjustment.getBatch_id() <= 0) {
            throw new IllegalArgumentException("يجب تحديد رقم التشغيلة (Batch) المتأثرة");
        }

        // 2. جلب معرف المستخدم الحالي من الجلسة النشطة
        try {
            int userId = SessionManager.getInstance().getCurrentUserId();
            adjustment.setUser_id(userId);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("يجب تسجيل الدخول لإجراء تسوية مخزون");
        }

        // 3. الحساب التلقائي للفرق (الكمية الفعلية - كمية النظام)
        // إذا كان الفعلي 10 والنظام 12، الفرق -2 (عجز)
        // إذا كان الفعلي 15 والنظام 12، الفرق +3 (زيادة)
        int calculatedDifference = adjustment.getActual_quantity() - adjustment.getSystem_quantity();
        adjustment.setDifference(calculatedDifference);

        // 4. ختم الوقت والتاريخ الحالي
        adjustment.setAdjustment_date(LocalDateTime.now());

        // 5. التمرير لطبقة DAO للتخزين
        return inventoryAdjustmentDAO.create(adjustment);
    }

    @Override
    public Optional<InventoryAdjustment> getAdjustmentById(int id) {
        return inventoryAdjustmentDAO.findById(id);
    }

    @Override
    public List<InventoryAdjustment> getAdjustmentsByBatch(int batchId) {
        return inventoryAdjustmentDAO.findByBatchId(batchId);
    }

    @Override
    public List<InventoryAdjustment> getAdjustmentsByUser(int userId) {
        return inventoryAdjustmentDAO.findByUserId(userId);
    }

    @Override
    public List<InventoryAdjustment> getAdjustmentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return inventoryAdjustmentDAO.findByDateRange(start, end);
    }
    
    @Override
    public List<InventoryAdjustment> findAll() {
        return inventoryAdjustmentDAO.findAll();
    }
    @Override
    public boolean processAdjustmentTransaction(InventoryAdjustment adjustment, int newQuantity) {
        // التحقق من صحة البيانات
        if (adjustment.getBatch_id() <= 0) {
            throw new IllegalArgumentException("يجب تحديد رقم التشغيلة (Batch) المتأثرة");
        }

        // جلب المستخدم (تأكد من استخدام الـ Import الصحيح للـ SessionManager هنا!)
        try {
            int userId = SessionManager.getInstance().getCurrentUser().getUser_id();
            adjustment.setUser_id(userId);
        } catch (Exception e) {
            throw new IllegalStateException("فشل في التعرف على المستخدم الحالي.");
        }

        int calculatedDifference = newQuantity - adjustment.getSystem_quantity();
        adjustment.setDifference(calculatedDifference);
        adjustment.setActual_quantity(newQuantity);
        adjustment.setAdjustment_date(LocalDateTime.now());

        // نمررها للـ DAO لينفذ الـ Transaction
        return inventoryAdjustmentDAO.executeAdjustmentTransaction(adjustment, newQuantity);
    }
}