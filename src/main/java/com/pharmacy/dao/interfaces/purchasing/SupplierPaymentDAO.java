// src/main/java/com/pharmacy/dao/interfaces/purchasing/SupplierPaymentDAO.java
package com.pharmacy.dao.interfaces.purchasing;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.purchasing.SupplierPayment;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for SupplierPayment entity.
 * Updated for financial reporting and payment tracking.
 */
public interface SupplierPaymentDAO extends GenericDAO<SupplierPayment, Integer> {
    
    // الدوال القديمة
    List<SupplierPayment> findBySupplierId(int supplierId);
    
    List<SupplierPayment> findByPurchaseId(int purchaseId);
    
    List<SupplierPayment> findByShiftId(int shiftId);

    // الدوال الجديدة (Micro-task 4.1)
    List<SupplierPayment> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<SupplierPayment> findByPaymentMethod(String method);
}