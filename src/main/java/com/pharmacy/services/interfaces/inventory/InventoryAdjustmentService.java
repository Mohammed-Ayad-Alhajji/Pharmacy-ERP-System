// مسار الملف: src/main/java/com/pharmacy/services/interfaces/inventory/InventoryAdjustmentService.java

package com.pharmacy.services.interfaces.inventory;

import com.pharmacy.models.inventory.InventoryAdjustment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * واجهة خدمة تسوية المخزون (Inventory Adjustment Service Interface).
 * مسؤولة عن إدارة عمليات تعديل الكميات يدوياً في المستودع ومعالجة الفوارق المخزنية.
 */
public interface InventoryAdjustmentService {

    /**
     * إنشاء سجل تسوية مخزون جديد وتحديث الكميات المرتبطة به.
     * @param adjustment كائن التسوية المحتوي على البيانات.
     * @return كائن التسوية بعد الحفظ في قاعدة البيانات.
     */
    Optional<InventoryAdjustment> createAdjustment(InventoryAdjustment adjustment);

    /**
     * جلب سجل تسوية محدد بواسطة المعرف الفريد.
     * @param id معرف سجل التسوية.
     * @return سجل التسوية المطلوب مغلفاً بـ Optional.
     */
    Optional<InventoryAdjustment> getAdjustmentById(int id);

    /**
     * جلب قائمة عمليات التسوية التي تمت على تشغيلة (Batch) محددة.
     * @param batchId معرف التشغيلة.
     * @return قائمة بسجلات التسوية المرتبطة.
     */
    List<InventoryAdjustment> getAdjustmentsByBatch(int batchId);

    /**
     * جلب قائمة عمليات التسوية التي قام بها مستخدم محدد.
     * @param userId معرف المستخدم.
     * @return قائمة بسجلات التسوية المرتبطة.
     */
    List<InventoryAdjustment> getAdjustmentsByUser(int userId);

    /**
     * جلب عمليات التسوية ضمن نطاق زمني محدد.
     * @param start تاريخ بداية النطاق.
     * @param end تاريخ نهاية النطاق.
     * @return قائمة بسجلات التسوية ضمن الفترة المحددة.
     */
    List<InventoryAdjustment> getAdjustmentsByDateRange(LocalDateTime start, LocalDateTime end);
    
    List<InventoryAdjustment> findAll();
    
    boolean processAdjustmentTransaction(InventoryAdjustment adjustment, int newQuantity);
    
}