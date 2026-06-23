// مسار الملف: src/main/java/com/pharmacy/services/impl/finance/InsurancePaymentServiceImpl.java

package com.pharmacy.services.impl.finance;

import com.pharmacy.dao.interfaces.finance.InsurancePaymentDAO;
import com.pharmacy.models.finance.InsurancePayment;
import com.pharmacy.services.interfaces.finance.InsurancePaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class InsurancePaymentServiceImpl implements InsurancePaymentService {

    private final InsurancePaymentDAO insurancePaymentDAO;

    public InsurancePaymentServiceImpl(InsurancePaymentDAO insurancePaymentDAO) {
        this.insurancePaymentDAO = insurancePaymentDAO;
    }

    @Override
    public Optional<InsurancePayment> createPayment(InsurancePayment payment) {
        // التحقق المحاسبي المزدوج (XOR Constraint)
        boolean hasInsurance = payment.getInsurance_id() != null && payment.getInsurance_id() > 0;
        boolean hasSale = payment.getSale_id() != null && payment.getSale_id() > 0;

        if ((hasInsurance && hasSale) || (!hasInsurance && !hasSale)) {
            throw new IllegalArgumentException("الدفعة يجب أن تكون مرتبطة إما بشركة تأمين أو بفاتورة، وليس كلاهما");
        }

        if (payment.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن ترتبط الدفعة بوردية مفتوحة");
        }

        if (payment.getAmount_paid() == null || payment.getAmount_paid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من الصفر");
        }

        if (payment.getPayment_method() == null || payment.getPayment_method().trim().isEmpty()) {
            throw new IllegalArgumentException("يجب تحديد طريقة الدفع");
        }

        if (payment.getPayment_date() == null) {
            payment.setPayment_date(LocalDateTime.now());
        }

        return insurancePaymentDAO.create(payment);
    }

    @Override
    public Optional<InsurancePayment> getPaymentById(int id) {
        return insurancePaymentDAO.findById(id);
    }

    @Override
    public List<InsurancePayment> getPaymentsByInsurance(int insuranceId) {
        return insurancePaymentDAO.findByInsuranceId(insuranceId);
    }

    @Override
    public List<InsurancePayment> getPaymentsBySale(int saleId) {
        return insurancePaymentDAO.findBySaleId(saleId);
    }

    @Override
    public List<InsurancePayment> getPaymentsByShift(int shiftId) {
        return insurancePaymentDAO.findByShiftId(shiftId);
    }

    @Override
    public List<InsurancePayment> getPaymentsByDateRange(LocalDate start, LocalDate end) {
        return insurancePaymentDAO.findByDateRange(start, end);
    }

    @Override
    public List<InsurancePayment> getPaymentsByMethod(String method) {
        return insurancePaymentDAO.findByPaymentMethod(method);
    }

    @Override
    public Optional<InsurancePayment> getPaymentByReference(String referenceNumber) {
        return insurancePaymentDAO.findByReferenceNumber(referenceNumber);
    }
}