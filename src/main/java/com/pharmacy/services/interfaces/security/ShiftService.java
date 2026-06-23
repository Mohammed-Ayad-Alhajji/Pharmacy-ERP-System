// مسار الملف: src/main/java/com/pharmacy/services/interfaces/security/ShiftService.java

package com.pharmacy.services.interfaces.security;

import com.pharmacy.models.finance.ShiftFinancialSummary;
import com.pharmacy.models.security.Shift;
import com.pharmacy.utils.exceptions.ShiftException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftService {
    
    Shift openShift(int userId, BigDecimal openingBalance) throws ShiftException;
    
    Shift closeShift(int shiftId, BigDecimal actualClosingBalance) throws ShiftException;
    
    Optional<Shift> getCurrentOpenShift(int userId);
    
    List<Shift> getShiftHistory(int userId, int limit, int offset);
    
    long getShiftHistoryCount(int userId);
    
    List<Shift> getShiftsByDateRange(LocalDateTime start, LocalDateTime end, int limit, int offset);
    
    // الدالة الجديدة لجلب الوردية برقمها
    Optional<Shift> getShiftById(int shiftId);
    ShiftFinancialSummary getShiftFinancialDetails(int shiftId);
}