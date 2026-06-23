// src/main/java/com/pharmacy/dao/interfaces/security/ShiftDAO.java
package com.pharmacy.dao.interfaces.security;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.finance.ShiftFinancialSummary;
import com.pharmacy.models.security.Shift;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Shift entity.
 * Updated for advanced HR reporting and audit capabilities.
 */
public interface ShiftDAO extends GenericDAO<Shift, Integer> {
    
    Optional<Shift> findOpenShiftByUserId(int userId);
    
    List<Shift> findByUserId(int userId);
    
    List<Shift> findByStatus(String status);
    
    List<Shift> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<Shift> findByUserIdAndDateRange(int userId, LocalDateTime startDate, LocalDateTime endDate);

    public Optional<Shift> getShiftById(int shiftId);
    ShiftFinancialSummary getShiftFinancialDetails(int shiftId);
}