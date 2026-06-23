package com.pharmacy.controllers.dashboard;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDetailDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDetailDAOImpl;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.models.pos.SaleDetail;
import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.impl.pos.SaleServiceImpl;
import com.pharmacy.services.impl.purchasing.PurchaseServiceImpl;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.services.interfaces.purchasing.PurchaseService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    // 1. المؤشرات الحيوية
    @FXML private Label todaySalesLabel;
    @FXML private Label todayPurchasesLabel;
    @FXML private Label receivablesLabel;
    @FXML private Label payablesLabel;

    // 2. الرسوم البيانية
    @FXML private LineChart<String, Number> salesLineChart;
    @FXML private PieChart topMedicinesChart;

    // 3. الجداول
    @FXML private TableView<RecentSaleDTO> recentSalesTable;
    @FXML private TableColumn<RecentSaleDTO, String> saleIdCol;
    @FXML private TableColumn<RecentSaleDTO, String> saleTimeCol;
    @FXML private TableColumn<RecentSaleDTO, String> saleValueCol;
    @FXML private TableColumn<RecentSaleDTO, String> saleMethodCol;

    @FXML private TableView<CashMovementDTO> cashMovementTable;
    @FXML private TableColumn<CashMovementDTO, String> cashTypeCol;
    @FXML private TableColumn<CashMovementDTO, String> cashDescCol;
    @FXML private TableColumn<CashMovementDTO, String> cashAmountCol;

    // الخدمات
    private SaleService saleService;
    private PurchaseService purchaseService;
    private MedicineService medicineService;
    private BatchService batchService;
    private SaleDetailDAOImpl saleDetailDAO;

    @FXML
    public void initialize() {
        // [إصلاح 1]: استخدام المتغير العام بدلاً من إنشاء متغير محلي يسبب تضارباً
        this.saleDetailDAO = new SaleDetailDAOImpl();
        
        saleService = new SaleServiceImpl(new SaleDAOImpl(), this.saleDetailDAO);
        purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), new PurchaseDetailDAOImpl());
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        batchService = new BatchServiceImpl(new BatchDAOImpl());

        // [إصلاح 2]: حل مشكلة الـ FXML بنقل خصائص تمدد الجدول برمجياً هنا
        recentSalesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cashMovementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        setupTableColumns();
        loadDashboardDataAsync();
    }

    private void setupTableColumns() {
        saleIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        saleTimeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        saleValueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        saleMethodCol.setCellValueFactory(new PropertyValueFactory<>("method"));

        cashTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        cashDescCol.setCellValueFactory(new PropertyValueFactory<>("desc"));
        cashAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
    }

    private void loadDashboardDataAsync() {
        Task<Void> dashboardTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                LocalDate today = LocalDate.now();
                LocalDate sevenDaysAgo = today.minusDays(6);
                
                LocalDate startOfYear = LocalDate.now().withDayOfYear(1);

                List<Sale> allSalesThisYear = saleService.getSalesByDateRange(startOfYear, today);
                List<Sale> weekSales = saleService.getSalesByDateRange(sevenDaysAgo, today);
                List<Sale> todaySales = weekSales.stream().filter(s -> s.getSale_date().toLocalDate().equals(today)).collect(Collectors.toList());
                
                List<Purchase> allPurchases = purchaseService.findAll();
                List<Purchase> todayPurchases = allPurchases.stream().filter(p -> p.getPurchase_date().toLocalDate().equals(today)).collect(Collectors.toList());

                // حساب المؤشرات الحيوية
                BigDecimal totalSalesToday = todaySales.stream()
                        .map(Sale::getTotal_amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalPurchasesToday = todayPurchases.stream()
                        .map(Purchase::getTotal_cost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalReceivables = allSalesThisYear.stream()
                        .map(s -> {
                            BigDecimal c = s.getTotal_customer_debt() != null ? s.getTotal_customer_debt() : BigDecimal.ZERO;
                            BigDecimal i = s.getTotal_insurance_debt() != null ? s.getTotal_insurance_debt() : BigDecimal.ZERO;
                            return c.add(i);
                        }).reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalPayables = allPurchases.stream()
                        .filter(p -> !"Paid".equalsIgnoreCase(p.getPayment_status()))
                        .map(Purchase::getTotal_cost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

                // الرسم البياني الخطي
                Map<LocalDate, BigDecimal> dailySalesMap = weekSales.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getSale_date().toLocalDate(),
                                Collectors.reducing(BigDecimal.ZERO, Sale::getTotal_amount, BigDecimal::add)
                        ));

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("المبيعات اليومية");
                DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE", new Locale("ar"));
                
                for (int i = 6; i >= 0; i--) {
                    LocalDate date = today.minusDays(i);
                    BigDecimal amount = dailySalesMap.getOrDefault(date, BigDecimal.ZERO);
                    series.getData().add(new XYChart.Data<>(date.format(dayFormatter), amount));
                }

                // الرسم الدائري
                List<SaleDetail> weekDetails = new ArrayList<>();
                for (Sale sale : weekSales) {
                    weekDetails.addAll(saleService.getSaleDetailsBySaleId(sale.getSale_id()));
                }

                List<Batch> allBatches = batchService.getAllBatches();
                List<Medicine> allMedicines = medicineService.getAllMedicines();

                Map<Integer, Integer> batchSalesCount = weekDetails.stream()
                        .collect(Collectors.groupingBy(SaleDetail::getBatch_id, Collectors.summingInt(SaleDetail::getQuantity_sold)));

                Map<Integer, Integer> medSalesCount = new HashMap<>();
                for (Map.Entry<Integer, Integer> entry : batchSalesCount.entrySet()) {
                    allBatches.stream().filter(b -> b.getBatch_id() == entry.getKey()).findFirst().ifPresent(batch -> {
                        medSalesCount.put(batch.getMed_id(), medSalesCount.getOrDefault(batch.getMed_id(), 0) + entry.getValue());
                    });
                }

                ObservableList<PieChart.Data> topMedicinesData = FXCollections.observableArrayList();
                medSalesCount.entrySet().stream()
                        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                        .limit(5)
                        .forEach(entry -> {
                            allMedicines.stream().filter(m -> m.getMed_id() == entry.getKey()).findFirst().ifPresent(med -> {
                                topMedicinesData.add(new PieChart.Data(med.getBrand_name(), entry.getValue()));
                            });
                        });

                // جداول المعاملات السريعة
                List<RecentSaleDTO> recentSalesList = todaySales.stream()
                        .sorted((s1, s2) -> s2.getSale_date().compareTo(s1.getSale_date()))
                        .limit(5)
                        .map(s -> new RecentSaleDTO(
                                "INV-" + s.getSale_id(),
                                s.getSale_date().toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a")),
                                String.format("%,.2f", s.getTotal_amount()),
                                s.getPayment_method() != null ? s.getPayment_method() : "نقدي"
                        )).collect(Collectors.toList());

                List<CashMovementDTO> cashMovements = new ArrayList<>();
                todaySales.stream()
                        .filter(s -> s.getTotal_patient_paid() != null && s.getTotal_patient_paid().compareTo(BigDecimal.ZERO) > 0)
                        .forEach(s -> cashMovements.add(new CashMovementDTO(
                                "سند قبض (مبيع)", "فاتورة مبيعات: INV-" + s.getSale_id(), "+ " + String.format("%,.2f", s.getTotal_patient_paid())
                        )));
                
                todayPurchases.stream()
                        .filter(p -> "Paid".equalsIgnoreCase(p.getPayment_status()))
                        .forEach(p -> cashMovements.add(new CashMovementDTO(
                                "سند صرف (مشتريات)", "فاتورة مورد رقم: " + p.getSupplier_invoice_number(), "- " + String.format("%,.2f", p.getTotal_cost())
                        )));

                // تحديث الواجهة
                Platform.runLater(() -> {
                    todaySalesLabel.setText(String.format("%,.2f", totalSalesToday));
                    todayPurchasesLabel.setText(String.format("%,.2f", totalPurchasesToday));
                    receivablesLabel.setText(String.format("%,.2f", totalReceivables));
                    payablesLabel.setText(String.format("%,.2f", totalPayables));

                    salesLineChart.getData().clear();
                    salesLineChart.getData().add(series);

                    topMedicinesChart.setData(topMedicinesData);

                    recentSalesTable.setItems(FXCollections.observableArrayList(recentSalesList));
                    cashMovementTable.setItems(FXCollections.observableArrayList(cashMovements));
                });

                return null;
            }
        };

        dashboardTask.setOnFailed(e -> {
            System.err.println("❌ خطأ في تحميل الداشبورد: " + dashboardTask.getException().getMessage());
            dashboardTask.getException().printStackTrace();
        });

        new Thread(dashboardTask).start();
    }

    public static class RecentSaleDTO {
        private final String id, time, value, method;
        public RecentSaleDTO(String id, String time, String value, String method) {
            this.id = id; this.time = time; this.value = value; this.method = method;
        }
        public String getId() { return id; }
        public String getTime() { return time; }
        public String getValue() { return value; }
        public String getMethod() { return method; }
    }

    public static class CashMovementDTO {
        private final String type, desc, amount;
        public CashMovementDTO(String type, String desc, String amount) {
            this.type = type; this.desc = desc; this.amount = amount;
        }
        public String getType() { return type; }
        public String getDesc() { return desc; }
        public String getAmount() { return amount; }
    }
}