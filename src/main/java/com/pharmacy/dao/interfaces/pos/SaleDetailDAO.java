// src/main/java/com/pharmacy/dao/interfaces/pos/SaleDetailDAO.java
package com.pharmacy.dao.interfaces.pos;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.pos.SaleDetail;
import java.util.List;

/**
 * Data Access Object interface for SaleDetail entity.
 * Updated for batch recall and health safety tracking.
 */
public interface SaleDetailDAO extends GenericDAO<SaleDetail, Integer> {
    
    // الدالة القديمة
    List<SaleDetail> findBySaleId(int saleId);

    // الدالة الجديدة (Micro-task 3.2)
    List<SaleDetail> findByBatchId(int batchId);
}