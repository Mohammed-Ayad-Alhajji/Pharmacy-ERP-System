package com.pharmacy.controllers.reports;

import com.pharmacy.dao.impl.reports.FinancialReportDAOImpl;
import com.pharmacy.models.reports.FinancialSummary;
import com.pharmacy.models.reports.TopSellingMedicine;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class FinancialReportMainController {

    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Button btnGenerate;

    // البطاقات العلوية (KPIs)
    @FXML private Label lblNetSales; // بدلاً من lblTotalRevenue
    @FXML private Label lblGrossSales; // جديد
    @FXML private Label lblTotalReturns; // تم نقله من الأسفل للأعلى
    @FXML private Label lblTotalCOGS;
    @FXML private Label lblTotalDeductions; // ستعرض المصاريف التشغيلية
    @FXML private Label lblNetProfit;

    // تفاصيل التبويب الثاني
    @FXML private Label lblDetailExpenses;
    @FXML private Label lblDetailDisposals;

    // تفاصيل التبويب الثالث
    @FXML private Label lblInventoryValue;

    // جدول الأدوية (التبويب الأول)
    @FXML private TableView<TopSellingMedicine> tvTopMedicines;
    @FXML private TableColumn<TopSellingMedicine, Integer> colMedIndex;
    @FXML private TableColumn<TopSellingMedicine, String> colMedName;
    @FXML private TableColumn<TopSellingMedicine, Integer> colMedQty;
    @FXML private TableColumn<TopSellingMedicine, String> colMedRevenue;
    @FXML private TableColumn<TopSellingMedicine, String> colMedProfit;

    private FinancialReportDAOImpl reportDAO;

    @FXML
    public void initialize() {
        reportDAO = new FinancialReportDAOImpl();

        // إعداد التواريخ الافتراضية (من أول الشهر الحالي إلى اليوم)
        dpStartDate.setValue(LocalDate.now().withDayOfMonth(1));
        dpEndDate.setValue(LocalDate.now());

        setupTableColumns();
        
        // توليد التقرير تلقائياً عند فتح الشاشة
        handleGenerateReport(null);
    }

    private void setupTableColumns() {
        colMedIndex.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(tvTopMedicines.getItems().indexOf(c.getValue()) + 1));
        colMedName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medicineName));
        colMedQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().totalQuantitySold));
        
        // تنسيق المبالغ في الجدول
        colMedRevenue.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().totalRevenue)));
        colMedProfit.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().totalProfit)));
    }

    @FXML
    private void handleGenerateReport(ActionEvent event) {
        if (dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            AlertManager.showWarning("تحذير", "يرجى تحديد فترة التقرير (من وإلى).");
            return;
        }

        LocalDateTime start = dpStartDate.getValue().atStartOfDay();
        LocalDateTime end = dpEndDate.getValue().atTime(LocalTime.MAX);

        if (start.isAfter(end)) {
            AlertManager.showWarning("تحذير", "تاريخ البداية يجب أن يكون قبل تاريخ النهاية.");
            return;
        }

        // تعطيل الزر وتغيير النص لتجنب التكرار أثناء التحميل
        btnGenerate.setDisable(true);
        btnGenerate.setText("جاري الحساب...");

        // استخدام Task لمعالجة الاستعلامات الضخمة في الخلفية دون تجميد الشاشة
        Task<ReportData> task = new Task<>() {
            @Override
            protected ReportData call() throws Exception {
                FinancialSummary summary = reportDAO.getFinancialSummary(start, end);
                List<TopSellingMedicine> topMeds = reportDAO.getTopSellingMedicines(start, end, 50); // جلب أفضل 50 دواء
                return new ReportData(summary, topMeds);
            }
        };

        task.setOnSucceeded(e -> {
            ReportData data = task.getValue();
            updateUI(data.summary, data.topMedicines);
            
            btnGenerate.setDisable(false);
            btnGenerate.setText("توليد التقرير 📊");
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            AlertManager.showError("خطأ", "حدث خطأ أثناء توليد التقرير المالي.");
            btnGenerate.setDisable(false);
            btnGenerate.setText("توليد التقرير 📊");
        });

        new Thread(task).start();
    }

    private void updateUI(FinancialSummary summary, List<TopSellingMedicine> topMeds) {
        Platform.runLater(() -> {
            // البطاقة 1: المبيعات والمرتجعات
            lblNetSales.setText(formatMoney(summary.netSales));
            lblGrossSales.setText("الخام: " + formatMoney(summary.totalRevenue));
            lblTotalReturns.setText("مرتجعات: " + formatMoney(summary.totalReturns));
            
            // البطاقة 2: التكلفة COGS
            lblTotalCOGS.setText(formatMoney(summary.totalCOGS));
            
            // البطاقة 3: المصاريف التشغيلية (مصاريف + إتلاف فقط)
            BigDecimal operatingLosses = summary.totalExpenses.add(summary.totalDisposalLoss);
            lblTotalDeductions.setText(formatMoney(operatingLosses));
            
            // البطاقة 4: صافي الربح الفعلي
            lblNetProfit.setText(formatMoney(summary.netProfit));

            // تحديث تفاصيل المصاريف (التبويب الثاني)
            lblDetailExpenses.setText(formatMoney(summary.totalExpenses));
            lblDetailDisposals.setText(formatMoney(summary.totalDisposalLoss));

            // تحديث قيمة المستودع (التبويب الثالث)
            lblInventoryValue.setText(formatMoney(summary.inventoryValue));

            // تحديث جدول الأدوية (التبويب الأول)
            tvTopMedicines.setItems(FXCollections.observableArrayList(topMeds));
        });
    }

    // دالة مساعدة لتنسيق المبالغ المالية بشكل مقروء
    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ل.س";
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return String.format("%,d ل.س", rounded.toBigInteger());
    }

    // كلاس مساعد لنقل البيانات من הـ Task
    private static class ReportData {
        FinancialSummary summary;
        List<TopSellingMedicine> topMedicines;

        ReportData(FinancialSummary summary, List<TopSellingMedicine> topMedicines) {
            this.summary = summary;
            this.topMedicines = topMedicines;
        }
    }
}