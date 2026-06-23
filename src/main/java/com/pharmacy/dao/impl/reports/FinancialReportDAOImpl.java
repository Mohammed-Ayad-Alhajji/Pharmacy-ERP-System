package com.pharmacy.dao.impl.reports;

import com.pharmacy.models.reports.FinancialSummary;
import com.pharmacy.models.reports.TopSellingMedicine;
import com.pharmacy.utils.DBConnectionManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FinancialReportDAOImpl {

    private final DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========================================================
    // 1. الدالة المركزية: جلب الخلاصة المالية (صافي الربح)
    // ========================================================
    public FinancialSummary getFinancialSummary(LocalDateTime startDate, LocalDateTime endDate) {
        FinancialSummary summary = new FinancialSummary();
        String startStr = startDate.format(dbFormatter);
        String endStr = endDate.format(dbFormatter);

        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            
            // 1. حساب إجمالي المبيعات (الخام)
            String sqlSales = "SELECT COALESCE(SUM(total_amount), 0) FROM Sales WHERE status != 'Cancelled' AND sale_date BETWEEN ? AND ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSales)) {
                ps.setString(1, startStr); ps.setString(2, endStr);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) summary.totalRevenue = rs.getBigDecimal(1);
            }

            // 2. حساب المرتجعات
            String sqlReturns = "SELECT COALESCE(SUM(patient_cash_refund + insurance_canceled_amount), 0) FROM Patient_Returns WHERE return_date BETWEEN ? AND ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlReturns)) {
                ps.setString(1, startStr); ps.setString(2, endStr);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) summary.totalReturns = rs.getBigDecimal(1);
            }

            // 3. حساب تكلفة البضاعة المباعة (COGS)
            String sqlCOGS = "SELECT COALESCE(SUM(sd.quantity_sold * b.buy_box_cost), 0) " +
                             "FROM Sale_Details sd " +
                             "JOIN Sales s ON sd.sale_id = s.sale_id " +
                             "JOIN Batches b ON sd.batch_id = b.batch_id " +
                             "WHERE s.status != 'Cancelled' AND s.sale_date BETWEEN ? AND ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlCOGS)) {
                ps.setString(1, startStr); ps.setString(2, endStr);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) summary.totalCOGS = rs.getBigDecimal(1);
            }

            // 4. حساب إجمالي المصروفات
            String sqlExpenses = "SELECT COALESCE(SUM(amount), 0) FROM Expenses WHERE expense_date BETWEEN ? AND ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlExpenses)) {
                ps.setString(1, startStr); ps.setString(2, endStr);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) summary.totalExpenses = rs.getBigDecimal(1);
            }

            // 5. حساب خسائر الإتلاف
            String sqlDisposals = "SELECT COALESCE(SUM(pharmacy_loss_amount), 0) FROM Disposals WHERE disposal_date BETWEEN ? AND ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDisposals)) {
                ps.setString(1, startStr); ps.setString(2, endStr);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) summary.totalDisposalLoss = rs.getBigDecimal(1);
            }

            // 6. حساب قيمة رأس المال المجمد في المستودع حالياً
            String sqlInventory = "SELECT COALESCE(SUM(quantity * buy_box_cost), 0) FROM Batches WHERE is_active = 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlInventory)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) summary.inventoryValue = rs.getBigDecimal(1);
            }

            // ==========================================
            // المعادلات المحاسبية الدقيقة
            // ==========================================
            
            // أ. صافي المبيعات (المبيعات الخام - المرتجعات)
            summary.netSales = summary.totalRevenue.subtract(summary.totalReturns);

            // ب. المصاريف التشغيلية (المصروفات + خسائر الإتلاف)
            BigDecimal operatingExpenses = summary.totalExpenses.add(summary.totalDisposalLoss);

            // ج. مجمل الربح (صافي المبيعات - تكلفة البضاعة)
            BigDecimal grossProfit = summary.netSales.subtract(summary.totalCOGS);

            // د. صافي الربح النهائي (مجمل الربح - المصاريف التشغيلية)
            summary.netProfit = grossProfit.subtract(operatingExpenses);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return summary;
    }

    // ========================================================
    // 2. دالة فرعية: جلب الأدوية الأكثر مبيعاً وربحية
    // ========================================================
    public List<TopSellingMedicine> getTopSellingMedicines(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        List<TopSellingMedicine> list = new ArrayList<>();
        String sql = "SELECT m.brand_name, " +
                     "SUM(sd.quantity_sold) as total_qty, " +
                     "SUM(sd.subtotal) as total_rev, " +
                     "SUM(sd.subtotal - (sd.quantity_sold * b.buy_box_cost)) as total_prof " +
                     "FROM Sale_Details sd " +
                     "JOIN Sales s ON sd.sale_id = s.sale_id " +
                     "JOIN Batches b ON sd.batch_id = b.batch_id " +
                     "JOIN Medicines m ON b.med_id = m.med_id " +
                     "WHERE s.status != 'Cancelled' AND s.sale_date BETWEEN ? AND ? " +
                     "GROUP BY m.med_id, m.brand_name " +
                     "ORDER BY total_prof DESC LIMIT ?";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, startDate.format(dbFormatter));
            ps.setString(2, endDate.format(dbFormatter));
            ps.setInt(3, limit);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TopSellingMedicine(
                        rs.getString("brand_name"),
                        rs.getInt("total_qty"),
                        rs.getBigDecimal("total_rev"),
                        rs.getBigDecimal("total_prof")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}