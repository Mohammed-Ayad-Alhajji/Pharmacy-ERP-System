package com.pharmacy.controllers.finance;

import com.pharmacy.dao.impl.finance.CustomerPaymentDAOImpl;
import com.pharmacy.dao.impl.pos.LocalCustomerDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDetailDAOImpl;
import com.pharmacy.models.finance.CustomerPayment;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.models.pos.SaleDetail;
import com.pharmacy.services.impl.finance.CustomerPaymentServiceImpl;
import com.pharmacy.services.impl.pos.LocalCustomerServiceImpl;
import com.pharmacy.services.impl.pos.SaleServiceImpl;
import com.pharmacy.services.interfaces.finance.CustomerPaymentService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class CustomerStatementController implements Initializable {

    @FXML private ComboBox<LocalCustomer> cbCustomer;
    @FXML private DatePicker dpStartDate, dpEndDate;
    
    @FXML private Label lblInitialBalance, lblTotalDebit, lblTotalCredit, lblFinalBalance;
    
    @FXML private TableView<StatementTransaction> tvStatement;
    @FXML private TableColumn<StatementTransaction, Integer> colIndex;
    @FXML private TableColumn<StatementTransaction, String> colDate, colType, colRef, colDetails;
    @FXML private TableColumn<StatementTransaction, BigDecimal> colDebit, colCredit, colBalance;

    private LocalCustomerService customerService;
    private SaleService saleService;
    private CustomerPaymentService paymentService;

    private ObservableList<LocalCustomer> customerList = FXCollections.observableArrayList();
    private ObservableList<StatementTransaction> tableData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectServices();
        setupDefaults();
        setupTable();
    }

    private void injectServices() {
        this.customerService = new LocalCustomerServiceImpl(new LocalCustomerDAOImpl());
        this.saleService = new SaleServiceImpl(new SaleDAOImpl(), new SaleDetailDAOImpl());
        this.paymentService = new CustomerPaymentServiceImpl(new CustomerPaymentDAOImpl());
    }

    private void setupDefaults() {
        customerList.setAll(customerService.getAllCustomers());
        setupSearchableComboBox(cbCustomer, customerList, LocalCustomer::getName);

        dpStartDate.setValue(LocalDate.now().withDayOfMonth(1));
        dpEndDate.setValue(LocalDate.now());
    }

    private void setupTable() {
        colIndex.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(tvStatement.getItems().indexOf(c.getValue()) + 1));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date.format(formatter)));
        
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type));
        colRef.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reference));
        colDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().details));
        
        colDebit.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().debit));
        colCredit.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().credit));
        colBalance.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().runningBalance));

        // تنسيق المدين بدون فواصل (أحمر لأنها ديون تزيد على العميل)
        colDebit.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.compareTo(BigDecimal.ZERO) == 0) { setText(""); setStyle(""); } 
                else { setText(formatMoney(item)); setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #c0392b; -fx-font-weight: bold;"); }
            }
        });

        // تنسيق الدائن بدون فواصل (أخضر لأنها دفعات تسدد الدين)
        colCredit.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.compareTo(BigDecimal.ZERO) == 0) { setText(""); setStyle(""); } 
                else { setText(formatMoney(item)); setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #27ae60; -fx-font-weight: bold;"); }
            }
        });

        // تنسيق الرصيد التراكمي 
        colBalance.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); setStyle(""); } 
                else {
                    BigDecimal displayVal = (item.compareTo(BigDecimal.ZERO) <= 0) ? BigDecimal.ZERO : item;
                    setText(formatMoney(displayVal)); 
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #fdf2e9;"); 
                }
            }
        });

        tvStatement.setItems(tableData);
    }

    private <T> void setupSearchableComboBox(ComboBox<T> comboBox, ObservableList<T> items, java.util.function.Function<T, String> textExtractor) {
        FilteredList<T> filteredItems = new FilteredList<>(items, p -> true);
        comboBox.setItems(filteredItems);

        comboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(T object) { return object != null ? textExtractor.apply(object) : ""; }
            @Override public T fromString(String string) {
                return items.stream().filter(item -> textExtractor.apply(item).equals(string)).findFirst().orElse(null);
            }
        });

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            final TextField editor = comboBox.getEditor();
            final T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null && textExtractor.apply(selected).equals(editor.getText())) return;
            filteredItems.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return textExtractor.apply(item).toLowerCase().contains(newValue.toLowerCase());
            });
            if (!filteredItems.isEmpty() && !comboBox.isShowing() && comboBox.getScene() != null && comboBox.getScene().getWindow() != null) {
                Platform.runLater(comboBox::show);
            }
        });
    }

    @FXML
    private void handleGenerateStatement() {
        LocalCustomer selectedCustomer = cbCustomer.getValue();
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        if (selectedCustomer == null) { AlertManager.showWarning("بيانات ناقصة", "يرجى اختيار العميل أولاً."); return; }
        if (start == null || end == null) { AlertManager.showWarning("بيانات ناقصة", "يرجى تحديد فترة التقرير."); return; }
        if (start.isAfter(end)) { AlertManager.showError("خطأ", "تاريخ البداية يجب أن يكون قبل تاريخ النهاية."); return; }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int cId = selectedCustomer.getCustomer_id();

                LocalDate veryEarly = LocalDate.of(2000, 1, 1);
                LocalDate adjustedEnd = end.plusDays(1);
                
                List<Sale> allSales = saleService.getSalesByDateRange(veryEarly, adjustedEnd);
                
                // جلب الدفعات اللاحقة المسجلة من شاشة المقبوضات
                List<CustomerPayment> allPayments = paymentService.getPaymentsByCustomer(cId);
                
                BigDecimal initialBalance = BigDecimal.ZERO;
                List<StatementTransaction> transactions = new ArrayList<>();

                // --- 1. معالجة فواتير المبيعات ---
                for (Sale sale : allSales) {
                    if (sale.getCustomer_id() != null && sale.getCustomer_id() == cId && sale.getSale_date() != null) {
                        
                        BigDecimal total = sale.getTotal_amount() != null ? sale.getTotal_amount() : BigDecimal.ZERO;
                        BigDecimal discount = sale.getDiscount_amount() != null ? sale.getDiscount_amount() : BigDecimal.ZERO;
                        BigDecimal rounding = sale.getRounding_adjustment() != null ? sale.getRounding_adjustment() : BigDecimal.ZERO;
                        
                        // استخراج حصة التأمين (إن وجدت)
                        BigDecimal insShare = BigDecimal.ZERO;
                        List<SaleDetail> details = saleService.getSaleDetailsBySaleId(sale.getSale_id());
                        for (SaleDetail detail : details) {
                            if(detail.getInsurance_share() != null) {
                                insShare = insShare.add(detail.getInsurance_share());
                            }
                        }

                        // الصافي المطلوب من العميل
                        BigDecimal netRequired = total.subtract(insShare).subtract(discount).add(rounding);
                        if (netRequired.compareTo(BigDecimal.ZERO) <= 0) continue; 

                        BigDecimal upfrontPayment = sale.getTotal_patient_paid() != null ? sale.getTotal_patient_paid() : BigDecimal.ZERO;

                        LocalDate sDate = sale.getSale_date().toLocalDate();
                        
                        if (sDate.isBefore(start)) {
                            initialBalance = initialBalance.add(netRequired).subtract(upfrontPayment); 
                        } else if (!sDate.isAfter(end)) {
                            // حركة المدين (الفاتورة) مع العبارة الاحترافية الجديدة
                            transactions.add(new StatementTransaction(
                                sale.getSale_date(), "فاتورة مبيعات", 
                                String.format("INV-%06d", sale.getSale_id()), 
                                "فاتورة مبيعات رقم (INV-" + sale.getSale_id() + ") - إثبات ذمة مالية", 
                                netRequired, BigDecimal.ZERO
                            ));

                            // حركة الدائن (الدفعة الأولى) مع العبارة الاحترافية الجديدة
                            if (upfrontPayment.compareTo(BigDecimal.ZERO) > 0) {
                                transactions.add(new StatementTransaction(
                                    sale.getSale_date().plusSeconds(1), 
                                    "دفعة نقدية (وقت البيع)", 
                                    String.format("INV-%06d", sale.getSale_id()), 
                                    "سداد جزء من قيمة الفاتورة نقداً وقت البيع", 
                                    BigDecimal.ZERO, upfrontPayment
                                ));
                            }
                        }
                    }
                }

                // --- 2. معالجة مقبوضات العملاء اللاحقة ---
                for (CustomerPayment pay : allPayments) {
                    if (pay.getPayment_date() == null) continue;
                    LocalDate payDate = pay.getPayment_date().toLocalDate();

                    if (payDate.isBefore(start)) {
                        initialBalance = initialBalance.subtract(pay.getAmount_paid()); 
                    } else if (!payDate.isAfter(end)) {
                        String ref = String.format("RCP-%06d", pay.getPayment_id());
                        
                        // إضافة الدفعة اللاحقة مع العبارة الاحترافية الجديدة
                        transactions.add(new StatementTransaction(
                            pay.getPayment_date(), "سند قبض (دفعة لاحقة)", ref, 
                            "سداد ذمة مالية سابقة - سند قبض نقدي", 
                            BigDecimal.ZERO, pay.getAmount_paid() 
                        ));
                    }
                }

                // 3. الترتيب الزمني
                Collections.sort(transactions);

                // 4. حساب الرصيد التراكمي 
                BigDecimal currentBalance = initialBalance;
                BigDecimal sumDebit = BigDecimal.ZERO;
                BigDecimal sumCredit = BigDecimal.ZERO;

                for (StatementTransaction tx : transactions) {
                    sumDebit = sumDebit.add(tx.debit);
                    sumCredit = sumCredit.add(tx.credit);
                    
                    currentBalance = currentBalance.add(tx.debit).subtract(tx.credit);
                    tx.runningBalance = currentBalance;
                }

                final BigDecimal finalInitial = initialBalance;
                final BigDecimal finalSumDebit = sumDebit;
                final BigDecimal finalSumCredit = sumCredit;
                final BigDecimal finalBalance = currentBalance;

                Platform.runLater(() -> {
                    tableData.setAll(transactions);
                    
                    lblInitialBalance.setText(formatMoney(finalInitial));
                    lblTotalDebit.setText(formatMoney(finalSumDebit));
                    lblTotalCredit.setText(formatMoney(finalSumCredit));
                    
                    if (finalBalance.compareTo(BigDecimal.ZERO) > 0) {
                        lblFinalBalance.setText(formatMoney(finalBalance));
                        lblFinalBalance.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 22px; -fx-font-weight: bold;");
                    } else {
                        lblFinalBalance.setText("0 ل.س");
                        lblFinalBalance.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 22px; -fx-font-weight: bold;");
                    }
                });
                return null;
            }
        };

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            Platform.runLater(() -> AlertManager.showError("خطأ", "فشل توليد كشف الحساب."));
        });
        
        new Thread(task).start();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ل.س";
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return String.format("%,d ل.س", rounded.toBigInteger());
    }

    @FXML 
    private void handleExportPDF() { 
        if (tableData.isEmpty()) {
            AlertManager.showWarning("تصدير فارغ", "لا يوجد بيانات لتصديرها. يرجى توليد كشف الحساب أولاً.");
            return;
        }
        
        LocalCustomer selectedCustomer = cbCustomer.getValue();
        String customerName = selectedCustomer != null ? selectedCustomer.getName() : "عميل";
        String title = "كشف حساب تفصيلي - " + customerName;
        
        // استدعاء دالة التصدير من كلاس ExportUtils
        com.pharmacy.utils.export.ExportUtils.exportToPDF(tvStatement, title);
    }
    
    @FXML 
    private void handleExportExcel() { 
        if (tableData.isEmpty()) {
            AlertManager.showWarning("تصدير فارغ", "لا يوجد بيانات لتصديرها. يرجى توليد كشف الحساب أولاً.");
            return;
        }
        
        LocalCustomer selectedCustomer = cbCustomer.getValue();
        String customerName = selectedCustomer != null ? selectedCustomer.getName() : "عميل";
        String fileName = "كشف_حساب_" + customerName;
        
        // استدعاء دالة التصدير من كلاس ExportUtils
        com.pharmacy.utils.export.ExportUtils.exportToExcel(tvStatement, fileName);
    }

    public static class StatementTransaction implements Comparable<StatementTransaction> {
        public LocalDateTime date;
        public String type;
        public String reference;
        public String details;
        public BigDecimal debit;   
        public BigDecimal credit;  
        public BigDecimal runningBalance;

        public StatementTransaction(LocalDateTime date, String type, String reference, String details, BigDecimal debit, BigDecimal credit) {
            this.date = date != null ? date : LocalDateTime.now();
            this.type = type;
            this.reference = reference;
            this.details = details;
            this.debit = debit != null ? debit : BigDecimal.ZERO;
            this.credit = credit != null ? credit : BigDecimal.ZERO;
            this.runningBalance = BigDecimal.ZERO;
        }

        @Override
        public int compareTo(StatementTransaction o) {
            return this.date.compareTo(o.date);
        }
    }
}