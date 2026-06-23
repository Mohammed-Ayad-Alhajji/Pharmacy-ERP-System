package com.pharmacy.models.reports;

import java.math.BigDecimal;

public class TopSellingMedicine {
    public String medicineName;
    public int totalQuantitySold;
    public BigDecimal totalRevenue;
    public BigDecimal totalProfit;

    public TopSellingMedicine(String medicineName, int totalQuantitySold, BigDecimal totalRevenue, BigDecimal totalProfit) {
        this.medicineName = medicineName;
        this.totalQuantitySold = totalQuantitySold;
        this.totalRevenue = totalRevenue;
        this.totalProfit = totalProfit;
    }
}