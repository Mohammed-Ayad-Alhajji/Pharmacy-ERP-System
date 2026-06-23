// مسار الملف: src/main/java/com/pharmacy/services/impl/inventory/DisposalServiceImpl.java

package com.pharmacy.services.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.DisposalDAO;
import com.pharmacy.models.inventory.Disposal;
import com.pharmacy.services.interfaces.inventory.DisposalService;

import com.pharmacy.security.SessionManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * تنفيذ خدمة إتلاف الأدوية.
 * يتم التدقيق هنا على التوازن المالي للعملية وضمان تسجيل المسؤول عن الإتلاف.
 */
public class DisposalServiceImpl implements DisposalService {

    private final DisposalDAO disposalDAO;

    /**
     * حقن التبعية لطبقة الوصول للبيانات.
     * @param disposalDAO الواجهة الخاصة بعمليات قاعدة البيانات للإتلاف.
     */
    public DisposalServiceImpl(DisposalDAO disposalDAO) {
        this.disposalDAO = disposalDAO;
    }

    @Override
    public Optional<Disposal> createDisposal(Disposal disposal) {
        // 1. التحقق من صحة البيانات الأساسية
        if (disposal.getBatch_id() <= 0) {
            throw new IllegalArgumentException("يجب تحديد رقم التشغيلة المراد إتلافها");
        }

        if (disposal.getQuantity_disposed() <= 0) {
            throw new IllegalArgumentException("كمية الإتلاف يجب أن تكون أكبر من الصفر");
        }

        // 2. تعيين المستخدم المسؤول من الجلسة الحالية
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUserId();
            disposal.setUser_id(currentUserId);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("يجب تسجيل الدخول لإجراء عملية إتلاف");
        }

        // 3. التحقق المالي وموازنة الحسابات (Financial Balancing)
        // القاعدة: إجمالي التكلفة = خسارة الصيدلية + تعويض المورد
        BigDecimal totalCost = disposal.getTotal_cost();
        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
            disposal.setTotal_cost(totalCost);
        }

        BigDecimal supplierCompensation = disposal.getSupplier_compensation_amount();
        if (supplierCompensation == null) {
            supplierCompensation = BigDecimal.ZERO;
            disposal.setSupplier_compensation_amount(supplierCompensation);
        }

        // حساب خسارة الصيدلية كفرق متبقي لضمان توازن القيد المالي
        BigDecimal calculatedPharmacyLoss = totalCost.subtract(supplierCompensation);
        disposal.setPharmacy_loss_amount(calculatedPharmacyLoss);

        // 4. ضبط توقيت العملية
        disposal.setDisposal_date(LocalDateTime.now());

        // 5. التنفيذ عبر طبقة DAO
        return disposalDAO.create(disposal);
    }

    @Override
    public Optional<Disposal> getDisposalById(int id) {
        return disposalDAO.findById(id);
    }

    @Override
    public List<Disposal> getDisposalsByBatch(int batchId) {
        return disposalDAO.findByBatchId(batchId);
    }

    @Override
    public List<Disposal> getDisposalsByUser(int userId) {
        return disposalDAO.findByUserId(userId);
    }

    @Override
    public List<Disposal> getDisposalsByDateRange(LocalDateTime start, LocalDateTime end) {
        return disposalDAO.findByDateRange(start, end);
    }
}