// مسار الملف: src/main/java/com/pharmacy/services/impl/purchasing/SupplierPaymentServiceImpl.java

package com.pharmacy.services.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.SupplierPaymentDAO;
import com.pharmacy.models.purchasing.SupplierPayment;
import com.pharmacy.services.interfaces.purchasing.SupplierPaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private final SupplierPaymentDAO supplierPaymentDAO;

    public SupplierPaymentServiceImpl(SupplierPaymentDAO supplierPaymentDAO) {
        this.supplierPaymentDAO = supplierPaymentDAO;
    }

    @Override
    public Optional<SupplierPayment> createPayment(SupplierPayment payment) {
        // 1. التحقق من وجود المورد
        boolean hasSupplier = payment.getSupplier_id() != null && payment.getSupplier_id() > 0;
        if (!hasSupplier) {
            throw new IllegalArgumentException("يجب اختيار المورد للدفعة المالية");
        }

        // 2. التحقق من الوردية
        if (payment.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن ترتبط الدفعة بوردية مفتوحة");
        }

        // 3. التحقق من المبلغ (يبقى موجباً دائماً لحماية قاعدة البيانات)
        if (payment.getAmount_paid() == null || payment.getAmount_paid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("مبلغ الدفعة يجب أن يكون أكبر من الصفر");
        }

        // 4. التحقق من نوع الحركة (Payment أو Refund)
        if (payment.getTransaction_type() == null || payment.getTransaction_type().trim().isEmpty()) {
            payment.setTransaction_type("Payment"); // القيمة الافتراضية
        } else if (!payment.getTransaction_type().equals("Payment") && !payment.getTransaction_type().equals("Refund")) {
            throw new IllegalArgumentException("نوع الحركة يجب أن يكون إما دفع (Payment) أو استرداد (Refund)");
        }

        // 5. ضبط تاريخ الدفعة إذا كان فارغاً
        if (payment.getPayment_date() == null) {
            payment.setPayment_date(LocalDateTime.now());
        }

        return supplierPaymentDAO.create(payment);
    }

    @Override public Optional<SupplierPayment> getPaymentById(int paymentId) { return supplierPaymentDAO.findById(paymentId); }
    @Override public List<SupplierPayment> getPaymentsBySupplier(int supplierId) { return supplierPaymentDAO.findBySupplierId(supplierId); }
    @Override public List<SupplierPayment> getPaymentsByPurchase(int purchaseId) { return supplierPaymentDAO.findByPurchaseId(purchaseId); }
    @Override public List<SupplierPayment> getPaymentsByShift(int shiftId) { return supplierPaymentDAO.findByShiftId(shiftId); }
    @Override public List<SupplierPayment> getPaymentsByDateRange(LocalDate start, LocalDate end) { return supplierPaymentDAO.findByDateRange(start, end); }
}