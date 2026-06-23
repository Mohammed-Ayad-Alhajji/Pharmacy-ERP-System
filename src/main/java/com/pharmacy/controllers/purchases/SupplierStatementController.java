package com.pharmacy.controllers.purchases;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDetailDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierPaymentDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierReturnDAOImpl;

import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.purchasing.Supplier;
import com.pharmacy.models.purchasing.SupplierPayment;
import com.pharmacy.models.purchasing.SupplierReturn;

import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.services.interfaces.purchasing.SupplierPaymentService;
import com.pharmacy.services.interfaces.purchasing.SupplierReturnService;
import com.pharmacy.services.interfaces.purchasing.SupplierService;

import com.pharmacy.services.impl.purchasing.PurchaseServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierPaymentServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierReturnServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierServiceImpl;

import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.transformation.FilteredList;

public class SupplierStatementController implements Initializable {

    @FXML private ComboBox<Supplier> cbSupplier;
    @FXML private DatePicker dpStartDate, dpEndDate;
    
    @FXML private Label lblInitialBalance, lblTotalDebit, lblTotalCredit, lblFinalBalance;
    
    @FXML private TableView<StatementTransaction> tvStatement;
    @FXML private TableColumn<StatementTransaction, Integer> colIndex;
    @FXML private TableColumn<StatementTransaction, String> colDate, colType, colRef, colDetails;
    @FXML private TableColumn<StatementTransaction, BigDecimal> colDebit, colCredit, colBalance;

    private SupplierService supplierService;
    private PurchaseService purchaseService;
    private SupplierPaymentService paymentService;
    private SupplierReturnService returnService;

    private ObservableList<Supplier> suppliersList = FXCollections.observableArrayList();
    private ObservableList<StatementTransaction> tableData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectServices();
        setupDefaults();
        setupTable();
    }

    private void injectServices() {
        this.supplierService = new SupplierServiceImpl(new SupplierDAOImpl());
        this.purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), new PurchaseDetailDAOImpl());
        this.paymentService = new SupplierPaymentServiceImpl(new SupplierPaymentDAOImpl());
        
        // تجهيز خدمة المرتجعات
        BatchDAOImpl batchDAO = new BatchDAOImpl();
        PurchaseDetailDAOImpl pdDAO = new PurchaseDetailDAOImpl();
        MedicineDAOImpl medDAO = new MedicineDAOImpl();
        this.returnService = new SupplierReturnServiceImpl(new SupplierReturnDAOImpl(), batchDAO, pdDAO, medDAO);
    }

    private void setupDefaults() {
        // تحميل الموردين وجعل القائمة قابلة للبحث
        suppliersList.setAll(supplierService.getAllSuppliers());
        setupSearchableComboBox(cbSupplier, suppliersList, Supplier::getName);

        // التواريخ الافتراضية
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

        // تلوين الخلايا الفارغة في المدين والدائن لتبدو مرتبة
        colDebit.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.compareTo(BigDecimal.ZERO) == 0) { setText(""); } 
                else { setText(item.toString()); setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #27ae60; -fx-font-weight: bold;"); }
            }
        });

        colCredit.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.compareTo(BigDecimal.ZERO) == 0) { setText(""); } 
                else { setText(item.toString()); setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #c0392b; -fx-font-weight: bold;"); }
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
        Supplier selectedSupplier = cbSupplier.getValue();
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        if (selectedSupplier == null) { AlertManager.showWarning("بيانات ناقصة", "يرجى اختيار المورد أولاً."); return; }
        if (start == null || end == null) { AlertManager.showWarning("بيانات ناقصة", "يرجى تحديد فترة التقرير."); return; }
        if (start.isAfter(end)) { AlertManager.showError("خطأ", "تاريخ البداية يجب أن يكون قبل تاريخ النهاية."); return; }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int sId = selectedSupplier.getSupplier_id();

                // 1. جلب كل الحركات للمورد من الجداول الثلاثة
                List<Purchase> allPurchases = purchaseService.getPurchasesBySupplier(sId);
                List<SupplierPayment> allPayments = paymentService.getPaymentsBySupplier(sId);
                
                // جلب المرتجعات (سنجلبها بناء على فواتير المورد)
                List<SupplierReturn> allReturns = new ArrayList<>();
                for (Purchase p : allPurchases) {
                    allReturns.addAll(returnService.getReturnsByPurchase(p.getPurchase_id()));
                }

                // 2. فصل الحركات (قبل فترة البحث "للرصيد الافتتاحي" ، وخلال فترة البحث "للجدول")
                BigDecimal initialBalance = BigDecimal.ZERO;
                List<StatementTransaction> transactions = new ArrayList<>();

                // --- معالجة المشتريات ---
                for (Purchase p : allPurchases) {
                    if (p.getPurchase_date() == null) continue;
                    LocalDate pDate = p.getPurchase_date().toLocalDate();
                    
                    if (pDate.isBefore(start)) {
                        initialBalance = initialBalance.add(p.getTotal_cost()); // المشتريات تزيد علينا (دائن)
                    } else if (!pDate.isAfter(end)) {
                        transactions.add(new StatementTransaction(p.getPurchase_date(), "فاتورة شراء", String.format("INV-%06d", p.getPurchase_id()), "فاتورة رقم المورد: " + (p.getSupplier_invoice_number() != null ? p.getSupplier_invoice_number() : ""), BigDecimal.ZERO, p.getTotal_cost()));
                    }
                }

                // --- معالجة الدفعات والاستردادات ---
                for (SupplierPayment pay : allPayments) {
                    if (pay.getPayment_date() == null) continue;
                    LocalDate payDate = pay.getPayment_date().toLocalDate();
                    boolean isRefund = "Refund".equals(pay.getTransaction_type());

                    if (payDate.isBefore(start)) {
                        if (isRefund) initialBalance = initialBalance.add(pay.getAmount_paid()); // استرداد (دائن)
                        else initialBalance = initialBalance.subtract(pay.getAmount_paid()); // دفع (مدين)
                    } else if (!payDate.isAfter(end)) {
                        String ref = pay.getReceipt_number() != null ? pay.getReceipt_number() : String.format("TRX-%06d", pay.getPayment_id());
                        String type = isRefund ? "استرداد نقدي" : "دفعة مصروفة";
                        BigDecimal debit = isRefund ? BigDecimal.ZERO : pay.getAmount_paid();
                        BigDecimal credit = isRefund ? pay.getAmount_paid() : BigDecimal.ZERO;
                        transactions.add(new StatementTransaction(pay.getPayment_date(), type, ref, "طريقة الدفع: " + pay.getPayment_method(), debit, credit));
                    }
                }

                // --- معالجة المرتجعات ---
                // --- معالجة المرتجعات ---
                for (SupplierReturn ret : allReturns) {
                    LocalDateTime retDateTime = ret.getReturn_date() != null ? ret.getReturn_date() : LocalDateTime.now();
                    LocalDate retDate = retDateTime.toLocalDate();

                    if (retDate.isBefore(start)) {
                        initialBalance = initialBalance.subtract(ret.getTotal_refund_value()); // المرتجع ينقص من حسابهم (مدين)
                    } else if (!retDate.isAfter(end)) {
                        // التعديل هنا: استخدام getSup_return_id() بدلاً من getReturn_id()
                        transactions.add(new StatementTransaction(
                                retDateTime, 
                                "مرتجع أدوية", 
                                String.format("RET-%06d", ret.getSup_return_id()), 
                                "إرجاع من فاتورة: INV-" + ret.getPurchase_id(), 
                                ret.getTotal_refund_value(), 
                                BigDecimal.ZERO
                        ));
                    }
                }

                // 3. ترتيب الحركات زمنياً من الأقدم للأحدث
                Collections.sort(transactions);

                // 4. حساب الرصيد التراكمي (Running Balance) والإجماليات
                BigDecimal currentBalance = initialBalance;
                BigDecimal sumDebit = BigDecimal.ZERO;
                BigDecimal sumCredit = BigDecimal.ZERO;

                for (StatementTransaction tx : transactions) {
                    sumDebit = sumDebit.add(tx.debit);
                    sumCredit = sumCredit.add(tx.credit);
                    
                    // الرصيد = السابق + الدائن (لهم) - المدين (لنا)
                    currentBalance = currentBalance.add(tx.credit).subtract(tx.debit);
                    tx.runningBalance = currentBalance;
                }

                // تجهيز المتغيرات للواجهة
                final BigDecimal finalInitial = initialBalance;
                final BigDecimal finalSumDebit = sumDebit;
                final BigDecimal finalSumCredit = sumCredit;
                final BigDecimal finalBalance = currentBalance;

                Platform.runLater(() -> {
                    tableData.setAll(transactions);
                    
                    lblInitialBalance.setText(finalInitial.toString() + " ل.س");
                    lblTotalDebit.setText(finalSumDebit.toString() + " ل.س");
                    lblTotalCredit.setText(finalSumCredit.toString() + " ل.س");
                    
                    if (finalBalance.compareTo(BigDecimal.ZERO) > 0) {
                        lblFinalBalance.setText(finalBalance.toString() + " ل.س (ذمة علينا)");
                        lblFinalBalance.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 22px; -fx-font-weight: bold;");
                    } else if (finalBalance.compareTo(BigDecimal.ZERO) < 0) {
                        lblFinalBalance.setText(finalBalance.abs().toString() + " ل.س (رصيد لصالحنا)");
                        lblFinalBalance.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 22px; -fx-font-weight: bold;");
                    } else {
                        lblFinalBalance.setText("0.00 ل.س (مسددة بالكامل)");
                        lblFinalBalance.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 22px; -fx-font-weight: bold;");
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

    @FXML 
    private void handleExportPDF() { 
        if (tableData.isEmpty()) {
            AlertManager.showWarning("تصدير فارغ", "لا يوجد بيانات لتصديرها. يرجى توليد كشف الحساب أولاً.");
            return;
        }
        
        Supplier selectedSupplier = cbSupplier.getValue();
        String supplierName = selectedSupplier != null ? selectedSupplier.getName() : "مورد";
        String title = "كشف حساب مورد تفصيلي - " + supplierName;
        
        // استدعاء دالة التصدير من كلاس ExportUtils
        com.pharmacy.utils.export.ExportUtils.exportToPDF(tvStatement, title);
    }

    @FXML 
    private void handleExportExcel() { 
        if (tableData.isEmpty()) {
            AlertManager.showWarning("تصدير فارغ", "لا يوجد بيانات لتصديرها. يرجى توليد كشف الحساب أولاً.");
            return;
        }
        
        Supplier selectedSupplier = cbSupplier.getValue();
        String supplierName = selectedSupplier != null ? selectedSupplier.getName() : "مورد";
        String fileName = "كشف_حساب_مورد_" + supplierName;
        
        // استدعاء دالة التصدير من كلاس ExportUtils
        com.pharmacy.utils.export.ExportUtils.exportToExcel(tvStatement, fileName);
    }

    // =========================================
    // كلاس التغليف لتوحيد حركات كشف الحساب
    // =========================================
    public static class StatementTransaction implements Comparable<StatementTransaction> {
        public LocalDateTime date;
        public String type;
        public String reference;
        public String details;
        public BigDecimal debit;   // مدين (حركة لصالح الصيدلية - دفع أو إرجاع)
        public BigDecimal credit;  // دائن (حركة لصالح المورد - شراء أو استرداد)
        public BigDecimal runningBalance; // الرصيد التراكمي

        public StatementTransaction(LocalDateTime date, String type, String reference, String details, BigDecimal debit, BigDecimal credit) {
            this.date = date != null ? date : LocalDateTime.now();
            this.type = type;
            this.reference = reference;
            this.details = details;
            this.debit = debit != null ? debit : BigDecimal.ZERO;
            this.credit = credit != null ? credit : BigDecimal.ZERO;
            this.runningBalance = BigDecimal.ZERO;
        }

        // الترتيب الزمني (الأقدم أولاً)
        @Override
        public int compareTo(StatementTransaction o) {
            return this.date.compareTo(o.date);
        }
    }
}