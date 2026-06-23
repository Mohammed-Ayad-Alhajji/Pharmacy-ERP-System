// src/main/java/com/pharmacy/dao/interfaces/inventory/InventoryAdjustmentDAO.java
package com.pharmacy.dao.interfaces.inventory;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.inventory.InventoryAdjustment;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object interface for InventoryAdjustment entity.
 * Updated to use LocalDateTime for precise shift and audit tracking.
 */
public interface InventoryAdjustmentDAO extends GenericDAO<InventoryAdjustment, Integer> {
    // تنفيذ العمليتين معاً في قاعدة البيانات
    boolean executeAdjustmentTransaction(InventoryAdjustment adjustment, int newQuantity);
    List<InventoryAdjustment> findByBatchId(int batchId);
    
    List<InventoryAdjustment> findByUserId(int userId);
    
    List<InventoryAdjustment> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}