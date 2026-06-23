// src/main/java/com/pharmacy/dao/interfaces/finance/InsurancePaymentDAO.java
package com.pharmacy.dao.interfaces.finance;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.finance.InsurancePayment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for InsurancePayment entity.
 * Includes methods for bank reconciliation and payment tracking.
 */
public interface InsurancePaymentDAO extends GenericDAO<InsurancePayment, Integer> {
    
    List<InsurancePayment> findByInsuranceId(int insuranceId);
    
    List<InsurancePayment> findBySaleId(int saleId);
    
    List<InsurancePayment> findByShiftId(int shiftId);
    
    List<InsurancePayment> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<InsurancePayment> findByPaymentMethod(String method);
    
    Optional<InsurancePayment> findByReferenceNumber(String referenceNumber);
}