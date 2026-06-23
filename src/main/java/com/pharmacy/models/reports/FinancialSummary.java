package com.pharmacy.models.reports;

import java.math.BigDecimal;

public class FinancialSummary {
    public BigDecimal totalRevenue = BigDecimal.ZERO;      // إجمالي المبيعات (الخام)
    public BigDecimal totalReturns = BigDecimal.ZERO;      // المرتجعات
    public BigDecimal netSales = BigDecimal.ZERO;          // صافي المبيعات (المبيعات - المرتجعات)
    
    public BigDecimal totalCOGS = BigDecimal.ZERO;         // تكلفة البضاعة المباعة
    public BigDecimal totalExpenses = BigDecimal.ZERO;     // المصروفات التشغيلية
    public BigDecimal totalDisposalLoss = BigDecimal.ZERO; // خسائر الإتلاف
    
    public BigDecimal netProfit = BigDecimal.ZERO;         // صافي الربح النهائي
    public BigDecimal inventoryValue = BigDecimal.ZERO;    // قيمة المستودع
}