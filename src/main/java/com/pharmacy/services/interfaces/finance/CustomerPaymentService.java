// مسار الملف: src/main/java/com/pharmacy/services/interfaces/finance/CustomerPaymentService.java

package com.pharmacy.services.interfaces.finance;

import com.pharmacy.models.finance.CustomerPayment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerPaymentService {

    Optional<CustomerPayment> createPayment(CustomerPayment payment);

    Optional<CustomerPayment> getPaymentById(int id);

    List<CustomerPayment> getPaymentsByCustomer(int customerId);

    List<CustomerPayment> getPaymentsBySale(int saleId);

    List<CustomerPayment> getPaymentsByShift(int shiftId);

    List<CustomerPayment> getPaymentsByDateRange(LocalDate start, LocalDate end);
}