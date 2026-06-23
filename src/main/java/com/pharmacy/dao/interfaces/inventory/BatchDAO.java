// src/main/java/com/pharmacy/dao/interfaces/inventory/BatchDAO.java
package com.pharmacy.dao.interfaces.inventory;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.inventory.Batch;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Batch entity.
 * Updated to support FEFO (First Expired, First Out) logic.
 */
public interface BatchDAO extends GenericDAO<Batch, Integer> {
    
    List<Batch> findByMedicineId(int medId);
    
    List<Batch> findActiveBatchesByMedicine(int medId);
    
    List<Batch> findNearExpiryBatches(int daysThreshold);
    
    Optional<Batch> findByBatchNumberAndMedicineId(String batchNumber, int medId);
    
    List<Batch> findValidBatchesOrderedByExpiry(int medId);
    
    Optional<Batch> getLastBatchByMedicineId(int medId);
    
}