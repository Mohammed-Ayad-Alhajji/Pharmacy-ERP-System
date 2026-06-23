// مسار الملف: src/main/java/com/pharmacy/services/impl/finance/CustomerPaymentServiceImpl.java

package com.pharmacy.services.impl.finance;

import com.pharmacy.dao.interfaces.finance.CustomerPaymentDAO;
import com.pharmacy.models.finance.CustomerPayment;
import com.pharmacy.services.interfaces.finance.CustomerPaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CustomerPaymentServiceImpl implements CustomerPaymentService {

    private final CustomerPaymentDAO customerPaymentDAO;

    public CustomerPaymentServiceImpl(CustomerPaymentDAO customerPaymentDAO) {
        this.customerPaymentDAO = customerPaymentDAO;
    }

    @Override
    public Optional<CustomerPayment> createPayment(CustomerPayment payment) {
        // التحقق المحاسبي المزدوج (XOR Constraint)
        boolean hasCustomer = payment.getCustomer_id() != null && payment.getCustomer_id() > 0;
        boolean hasSale = payment.getSale_id() != null && payment.getSale_id() > 0;

        if ((hasCustomer && hasSale) || (!hasCustomer && !hasSale)) {
            throw new IllegalArgumentException("الدفعة يجب أن تكون مرتبطة إما بعميل أو بفاتورة، وليس كلاهما");
        }

        if (payment.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن ترتبط الدفعة بوردية مفتوحة");
        }

        if (payment.getAmount_paid() == null || payment.getAmount_paid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من الصفر");
        }

        if (payment.getPayment_date() == null) {
            payment.setPayment_date(LocalDateTime.now());
        }

        return customerPaymentDAO.create(payment);
    }

    @Override
    public Optional<CustomerPayment> getPaymentById(int id) {
        return customerPaymentDAO.findById(id);
    }

    @Override
    public List<CustomerPayment> getPaymentsByCustomer(int customerId) {
        return customerPaymentDAO.findByCustomerId(customerId);
    }

    @Override
    public List<CustomerPayment> getPaymentsBySale(int saleId) {
        return customerPaymentDAO.findBySaleId(saleId);
    }

    @Override
    public List<CustomerPayment> getPaymentsByShift(int shiftId) {
        return customerPaymentDAO.findByShiftId(shiftId);
    }

    @Override
    public List<CustomerPayment> getPaymentsByDateRange(LocalDate start, LocalDate end) {
        return customerPaymentDAO.findByDateRange(start, end);
    }
}