// src/main/java/com/pharmacy/dao/interfaces/finance/CustomerPaymentDAO.java
package com.pharmacy.dao.interfaces.finance;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.finance.CustomerPayment;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for CustomerPayment entity.
 */
public interface CustomerPaymentDAO extends GenericDAO<CustomerPayment, Integer> {
    
    List<CustomerPayment> findByCustomerId(int customerId);
    
    List<CustomerPayment> findBySaleId(int saleId);
    
    List<CustomerPayment> findByShiftId(int shiftId);
    
    List<CustomerPayment> findByDateRange(LocalDate startDate, LocalDate endDate);
}