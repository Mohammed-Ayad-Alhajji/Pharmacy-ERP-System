// src/main/java/com/pharmacy/dao/interfaces/purchasing/SupplierReturnDAO.java
package com.pharmacy.dao.interfaces.purchasing;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.purchasing.SupplierReturn;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for SupplierReturn entity.
 * Updated for financial reporting and security auditing.
 */
public interface SupplierReturnDAO extends GenericDAO<SupplierReturn, Integer> {
    
    // الدوال القديمة
    List<SupplierReturn> findByPurchaseId(int purchaseId);
    
    List<SupplierReturn> findByBatchId(int batchId);
    
    List<SupplierReturn> findByStatus(String status);

    // الدوال الجديدة (Micro-task 4.1)
    List<SupplierReturn> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<SupplierReturn> findByUserId(int userId);
}