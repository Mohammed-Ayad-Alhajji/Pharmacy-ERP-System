// مسار الملف: src/main/java/com/pharmacy/services/interfaces/purchasing/SupplierReturnService.java

package com.pharmacy.services.interfaces.purchasing;

import com.pharmacy.models.purchasing.SupplierReturn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplierReturnService {

    Optional<SupplierReturn> createReturn(SupplierReturn supplierReturn);
    
    // الدالة المعمارية الجديدة لمعالجة عدة مرتجعات دفعة واحدة بأمان
    boolean processMultipleReturns(List<SupplierReturn> returns);

    boolean updateReturn(SupplierReturn supplierReturn);

    boolean deleteReturn(int returnId);

    Optional<SupplierReturn> getReturnById(int returnId);

    List<SupplierReturn> getReturnsByPurchase(int purchaseId);

    List<SupplierReturn> getReturnsByBatch(int batchId);

    List<SupplierReturn> getReturnsByStatus(String status);

    List<SupplierReturn> getReturnsByDateRange(LocalDate start, LocalDate end);

    List<SupplierReturn> getReturnsByUser(int userId);
}