// src/main/java/com/pharmacy/dao/interfaces/purchasing/PurchaseDAO.java
package com.pharmacy.dao.interfaces.purchasing;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.purchasing.Purchase;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for Purchase entity.
 */
public interface PurchaseDAO extends GenericDAO<Purchase, Integer> {
    
    List<Purchase> findBySupplierId(int supplierId);
    
    List<Purchase> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<Purchase> findByPaymentStatus(String status);
    
    List<Purchase> findByShiftId(int userId);
    
    boolean updatePaymentStatus(int purchaseId, String status);
}