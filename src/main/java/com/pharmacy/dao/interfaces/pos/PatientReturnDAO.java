// src/main/java/com/pharmacy/dao/interfaces/pos/PatientReturnDAO.java
package com.pharmacy.dao.interfaces.pos;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.pos.PatientReturn;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for PatientReturn entity.
 */
public interface PatientReturnDAO extends GenericDAO<PatientReturn, Integer> {
    
    List<PatientReturn> findBySaleDetailId(int detailId);
    
    List<PatientReturn> findByShiftId(int shiftId);
    
    List<PatientReturn> findByDateRange(LocalDate startDate, LocalDate endDate);
}