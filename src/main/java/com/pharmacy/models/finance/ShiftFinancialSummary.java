package com.pharmacy.models.finance;

import java.math.BigDecimal;

public class ShiftFinancialSummary {
    public int shiftId;
    public BigDecimal openingBalance = BigDecimal.ZERO;
    
    // الموجب (+)
    public BigDecimal cashSales = BigDecimal.ZERO;
    public BigDecimal customerReceipts = BigDecimal.ZERO;
    public BigDecimal insuranceReceipts = BigDecimal.ZERO;
    public BigDecimal supplierRefunds = BigDecimal.ZERO;
    
    // السالب (-)
    public BigDecimal patientRefunds = BigDecimal.ZERO;
    public BigDecimal supplierPayments = BigDecimal.ZERO;
    public BigDecimal expenses = BigDecimal.ZERO;
    
    // النتائج
    public BigDecimal expectedBalance = BigDecimal.ZERO;
    public BigDecimal actualBalance = BigDecimal.ZERO;
    public BigDecimal difference = BigDecimal.ZERO;
}