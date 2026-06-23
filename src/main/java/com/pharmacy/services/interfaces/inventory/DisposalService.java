// المسار: src/main/java/com/pharmacy/services/interfaces/inventory/DisposalService.java

package com.pharmacy.services.interfaces.inventory;

import com.pharmacy.models.inventory.Disposal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * واجهة خدمة إتلاف الأدوية (Inventory Disposal Service Interface).
 * مسؤولة عن إدارة عمليات استبعاد الأدوية من المخزون لأسباب تقنية أو طبية (انتهاء صلاحية، تلف، إلخ).
 */
public interface DisposalService {

    /**
     * إنشاء سجل إتلاف جديد وتحديث كميات المخزون المرتبطة.
     * @param disposal كائن الإتلاف المحتوي على البيانات.
     * @return كائن الإتلاف بعد الحفظ في قاعدة البيانات مغلفاً بـ Optional.
     */
    Optional<Disposal> createDisposal(Disposal disposal);

    /**
     * جلب سجل إتلاف محدد بواسطة المعرف الفريد.
     * @param id معرف سجل الإتلاف.
     * @return سجل الإتلاف المطلوب مغلفاً بـ Optional.
     */
    Optional<Disposal> getDisposalById(int id);

    /**
     * جلب كافة عمليات الإتلاف التي تمت على تشغيلة (Batch) محددة.
     * @param batchId معرف التشغيلة.
     * @return قائمة بسجلات الإتلاف المرتبطة.
     */
    List<Disposal> getDisposalsByBatch(int batchId);

    /**
     * جلب كافة عمليات الإتلاف التي نفذها مستخدم محدد.
     * @param userId معرف المستخدم.
     * @return قائمة بسجلات الإتلاف المرتبطة.
     */
    List<Disposal> getDisposalsByUser(int userId);

    /**
     * جلب سجلات الإتلاف ضمن نطاق زمني محدد.
     * @param start تاريخ بداية النطاق الزمني.
     * @param end تاريخ نهاية النطاق الزمني.
     * @return قائمة بسجلات الإتلاف ضمن الفترة المحددة.
     */
    List<Disposal> getDisposalsByDateRange(LocalDateTime start, LocalDateTime end);
}