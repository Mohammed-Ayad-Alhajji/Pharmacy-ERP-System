// src/main/java/com/pharmacy/dao/interfaces/purchasing/PurchaseDetailDAO.java
package com.pharmacy.dao.interfaces.purchasing;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.purchasing.PurchaseDetail;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for PurchaseDetail entity.
 */
public interface PurchaseDetailDAO extends GenericDAO<PurchaseDetail, Integer> {
    
    List<PurchaseDetail> findByPurchaseId(int purchaseId);
    
    Optional<PurchaseDetail> findByBatchId(int batchId);
}