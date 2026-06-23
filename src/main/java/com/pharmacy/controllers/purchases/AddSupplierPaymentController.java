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
import com.pharmacy.models.security.Shift;
import com.pharmacy.security.SessionManager;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AddSupplierPaymentController implements Initializable {

    @FXML private Label lblTitle, lblInvoiceBalance, lblHint;
    @FXML private ComboBox<Supplier> cbSupplier;
    @FXML private ComboBox<Purchase> cbInvoice;
    @FXML private TextField txtAmount;
    @FXML private Button btnSave;

    private SupplierPaymentService paymentService;
    private SupplierService supplierService;
    private PurchaseService purchaseService;
    private SupplierReturnService returnService;
    
    private String transactionType; 
    private SupplierPaymentsController parentController;
    
    private ObservableList<Supplier> allSuppliers = FXCollections.observableArrayList();
    private ObservableList<Purchase> supplierInvoices = FXCollections.observableArrayList();
    private BigDecimal currentInvoiceRemainingBalance = BigDecimal.ZERO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.paymentService = new SupplierPaymentServiceImpl(new SupplierPaymentDAOImpl());
        this.supplierService = new SupplierServiceImpl(new SupplierDAOImpl());
        this.purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), new PurchaseDetailDAOImpl());
        this.returnService = new SupplierReturnServiceImpl(new SupplierReturnDAOImpl(), new BatchDAOImpl(), new PurchaseDetailDAOImpl(), new MedicineDAOImpl());

        loadSuppliers();
        setupSearchableComboBox(cbSupplier, allSuppliers, Supplier::getName);
        setupSearchableComboBox(cbInvoice, supplierInvoices, p -> String.format("INV-%06d (الإجمالي: %s)", p.getPurchase_id(), p.getTotal_cost()));

        setupListeners();
    }

    public void initData(String type, SupplierPaymentsController parent) {
        this.transactionType = type;
        this.parentController = parent;

        if ("Refund".equals(type)) {
            lblTitle.setText("استلام استرداد نقدي من مورد (+)");
            btnSave.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
            lblHint.setText("* هذا المبلغ سيتم إضافته إلى درج الكاشير للوردية الحالية.");
        } else {
            lblTitle.setText("صرف دفعة مالية لمورد (-)");
            btnSave.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
            lblHint.setText("* سيتم خصم هذا المبلغ من درج الكاشير للوردية الحالية.");
        }
    }

    private void loadSuppliers() {
        allSuppliers.setAll(supplierService.getAllSuppliers());
    }

    private void setupListeners() {
        cbSupplier.valueProperty().addListener((obs, oldVal, newVal) -> {
            supplierInvoices.clear();
            cbInvoice.setValue(null);
            lblInvoiceBalance.setVisible(false);
            lblInvoiceBalance.setManaged(false);
            
            if (newVal != null) {
                loadSupplierInvoices(newVal.getSupplier_id());
            }
        });

        cbInvoice.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentInvoiceRemainingBalance = calculateRemainingBalance(newVal.getPurchase_id(), newVal.getTotal_cost());
                lblInvoiceBalance.setText(String.format("الرصيد المتبقي للفاتورة: %s ل.س", currentInvoiceRemainingBalance));
                lblInvoiceBalance.setVisible(true); 
                lblInvoiceBalance.setManaged(true);
            } else {
                currentInvoiceRemainingBalance = BigDecimal.ZERO;
                lblInvoiceBalance.setVisible(false); 
                lblInvoiceBalance.setManaged(false);
            }
        });
    }

    private void loadSupplierInvoices(int supplierId) {
        // تأكد من اسم الدالة في السيرفس عندك (قد تكون getPurchasesBySupplier)
        List<Purchase> purchases = purchaseService.getPurchasesBySupplier(supplierId);
        
        List<Purchase> filtered = purchases.stream()
                .filter(p -> {
                    // نحسب رصيد كل فاتورة لحظياً
                    BigDecimal bal = calculateRemainingBalance(p.getPurchase_id(), p.getTotal_cost());
                    
                    if ("Payment".equals(transactionType)) {
                        return bal.compareTo(BigDecimal.ZERO) > 0; // جلب الفواتير التي علينا لها ديون فقط
                    } else {
                        return bal.compareTo(BigDecimal.ZERO) < 0; // جلب الفواتير التي لها مرتجعات (رصيد سالب) فقط
                    }
                })
                .sorted(Comparator.comparing(Purchase::getPurchase_date))
                .collect(Collectors.toList());
                
        supplierInvoices.setAll(filtered);
    }

    // المعادلة الرياضية المباشرة لحساب رصيد الفاتورة
    private BigDecimal calculateRemainingBalance(int purchaseId, BigDecimal totalCost) {
        // 1. المرتجعات
        List<SupplierReturn> returns = returnService.getReturnsByPurchase(purchaseId);
        BigDecimal totalReturns = returns.stream().map(SupplierReturn::getTotal_refund_value).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. الدفعات والاستردادات
        List<SupplierPayment> payments = paymentService.getPaymentsByPurchase(purchaseId);
        BigDecimal totalPaid = payments.stream()
                .filter(p -> "Payment".equals(p.getTransaction_type()))
                .map(SupplierPayment::getAmount_paid).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashRefunds = payments.stream()
                .filter(p -> "Refund".equals(p.getTransaction_type()))
                .map(SupplierPayment::getAmount_paid).reduce(BigDecimal.ZERO, BigDecimal::add);

        // الرصيد = الإجمالي - المرتجعات - الدفعات + الاستردادات النقدية
        return totalCost.subtract(totalReturns).subtract(totalPaid).add(cashRefunds);
    }

    @FXML
    private void handleSave() {
        Supplier selectedSupplier = cbSupplier.getValue();
        Purchase selectedInvoice = cbInvoice.getValue();
        
        if (selectedSupplier == null) {
            AlertManager.showWarning("بيانات ناقصة", "يرجى اختيار المورد."); return;
        }

        // 1. إجبار المستخدم على اختيار الفاتورة دائماً (للدفع أو الاسترداد)
        if (selectedInvoice == null) {
            AlertManager.showWarning("بيانات ناقصة", "يرجى اختيار الفاتورة المراد التعامل معها."); return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(txtAmount.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertManager.showError("خطأ إدخال", "يرجى إدخال مبلغ صحيح أكبر من الصفر."); return;
        }

        // 2. المنطق الرياضي الصارم للرصيد
        if ("Payment".equals(transactionType)) {
            if (amount.compareTo(currentInvoiceRemainingBalance) > 0) {
                AlertManager.showError("تجاوز الحد", "المبلغ المدفوع يتجاوز الرصيد المتبقي للفاتورة (" + currentInvoiceRemainingBalance + " ل.س).");
                return;
            }
        } else if ("Refund".equals(transactionType)) {
            BigDecimal maxRefundable = currentInvoiceRemainingBalance.abs();
            if (amount.compareTo(maxRefundable) > 0) {
                AlertManager.showError("تجاوز الحد", "المبلغ المسترد يتجاوز قيمة المرتجعات المتاحة لهذه الفاتورة (" + maxRefundable + " ل.س).");
                return;
            }
        }

        Shift currentShift = SessionManager.getInstance().getCurrentShift();
        if (currentShift == null || currentShift.getShift_id() <= 0) {
            AlertManager.showError("خطأ أمان", "يجب فتح وردية أولاً لتسجيل حركات الصندوق."); return;
        }

        // ========================================================
        // 🌟 إضافة رسالة التأكيد (Confirmation Dialog) 🌟
        // ========================================================
        String confirmMsg = "Payment".equals(transactionType) ? 
            "هل أنت متأكد من صرف دفعة نقدية بقيمة ( " + amount + " ل.س )؟\nسيتم خصم هذا المبلغ من درج الكاشير." :
            "هل أنت متأكد من استلام استرداد نقدي بقيمة ( " + amount + " ل.س )؟\nسيتم إضافة هذا المبلغ إلى درج الكاشير.";
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("تأكيد العملية المالية");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(confirmMsg);
        
        // تعريب الأزرار
        ButtonType btnYes = new ButtonType("نعم، تأكيد", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("إلغاء", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnYes, btnNo);
        
        // إيقاف الكود حتى يختار المستخدم
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (!result.isPresent() || result.get() != btnYes) {
            return; // المستخدم ضغط إلغاء، يتم إيقاف الحفظ فوراً
        }
        // ========================================================

        try {
            SupplierPayment payment = new SupplierPayment();
            payment.setSupplier_id(selectedSupplier.getSupplier_id());
            payment.setPurchase_id(selectedInvoice.getPurchase_id());
            payment.setShift_id(currentShift.getShift_id());
            payment.setAmount_paid(amount);
            
            String prefix = "Refund".equals(transactionType) ? "REC" : "PAY";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            payment.setReceipt_number(prefix + "-" + timestamp);
            
            payment.setTransaction_type(transactionType);
            payment.setPayment_method("نقدي"); 
            payment.setPayment_date(LocalDateTime.now());
            
            paymentService.createPayment(payment);

            // 3. تحديث حالة الفاتورة
            BigDecimal newBalance = calculateRemainingBalance(selectedInvoice.getPurchase_id(), selectedInvoice.getTotal_cost());
            if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                purchaseService.updatePaymentStatus(selectedInvoice.getPurchase_id(), "Paid");
            } else {
                purchaseService.updatePaymentStatus(selectedInvoice.getPurchase_id(), "Partial");
            }

            AlertManager.showSuccess("نجاح", "تم تسجيل الحركة المالية بنجاح.");
            if (parentController != null) parentController.loadInitialData();
            handleCancel();

        } catch (Exception e) {
            e.printStackTrace();
            AlertManager.showError("خطأ", "حدث خطأ أثناء حفظ الحركة: " + e.getMessage());
        }
    }

    @FXML private void handleCancel() { ((Stage) btnSave.getScene().getWindow()).close(); }

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
}