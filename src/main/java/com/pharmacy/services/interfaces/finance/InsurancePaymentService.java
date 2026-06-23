// مسار الملف: src/main/java/com/pharmacy/services/interfaces/finance/InsurancePaymentService.java

package com.pharmacy.services.interfaces.finance;

import com.pharmacy.models.finance.InsurancePayment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InsurancePaymentService {

    Optional<InsurancePayment> createPayment(InsurancePayment payment);

    Optional<InsurancePayment> getPaymentById(int id);

    List<InsurancePayment> getPaymentsByInsurance(int insuranceId);

    List<InsurancePayment> getPaymentsBySale(int saleId);

    List<InsurancePayment> getPaymentsByShift(int shiftId);

    List<InsurancePayment> getPaymentsByDateRange(LocalDate start, LocalDate end);

    List<InsurancePayment> getPaymentsByMethod(String method);

    Optional<InsurancePayment> getPaymentByReference(String referenceNumber);
}