// مسار الملف: src/main/java/com/pharmacy/services/interfaces/purchasing/SupplierPaymentService.java

package com.pharmacy.services.interfaces.purchasing;

import com.pharmacy.models.purchasing.SupplierPayment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplierPaymentService {

    Optional<SupplierPayment> createPayment(SupplierPayment payment);

    Optional<SupplierPayment> getPaymentById(int paymentId);

    List<SupplierPayment> getPaymentsBySupplier(int supplierId);

    List<SupplierPayment> getPaymentsByPurchase(int purchaseId);

    List<SupplierPayment> getPaymentsByShift(int shiftId);

    List<SupplierPayment> getPaymentsByDateRange(LocalDate start, LocalDate end);
}