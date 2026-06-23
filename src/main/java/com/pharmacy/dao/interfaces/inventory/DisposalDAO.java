// src/main/java/com/pharmacy/dao/interfaces/inventory/DisposalDAO.java
package com.pharmacy.dao.interfaces.inventory;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.inventory.Disposal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object interface for Disposal entity.
 * Updated to use LocalDateTime for precise shift and audit tracking.
 */
public interface DisposalDAO extends GenericDAO<Disposal, Integer> {
    
    List<Disposal> findByBatchId(int batchId);
    
    List<Disposal> findByUserId(int userId);
    
    List<Disposal> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}