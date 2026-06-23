// src/main/java/com/pharmacy/dao/interfaces/finance/ExpenseDAO.java
package com.pharmacy.dao.interfaces.finance;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.finance.Expense;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for Expense entity.
 */
public interface ExpenseDAO extends GenericDAO<Expense, Integer> {
    
    List<Expense> findByCategoryId(int categoryId);
    
    List<Expense> findByShiftId(int shiftId);
    
    List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate);
}