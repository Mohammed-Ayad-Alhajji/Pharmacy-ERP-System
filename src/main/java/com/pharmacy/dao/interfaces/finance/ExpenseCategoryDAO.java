// src/main/java/com/pharmacy/dao/interfaces/finance/ExpenseCategoryDAO.java
package com.pharmacy.dao.interfaces.finance;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.finance.ExpenseCategory;
import java.util.Optional;

/**
 * Data Access Object interface for ExpenseCategory entity.
 */
public interface ExpenseCategoryDAO extends GenericDAO<ExpenseCategory, Integer> {
    
    Optional<ExpenseCategory> findByName(String name);
}