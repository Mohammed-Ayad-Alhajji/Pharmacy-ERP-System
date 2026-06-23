// src/main/java/com/pharmacy/dao/interfaces/pos/SaleDAO.java
package com.pharmacy.dao.interfaces.pos;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.pos.Sale;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for Sale entity.
 * Updated to include financial and insurance tracking methods.
 */
public interface SaleDAO extends GenericDAO<Sale, Integer> {
    
    // الدوال القديمة
    List<Sale> findByShiftId(int shiftId);
    
    List<Sale> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<Sale> findByCustomerId(int customerId);

    // الدوال الجديدة (Micro-task 3.2)
    List<Sale> findByInsuranceId(int insuranceId);
    
    List<Sale> findByStatus(String status);
    
    List<Sale> findByPaymentMethod(String method);
}