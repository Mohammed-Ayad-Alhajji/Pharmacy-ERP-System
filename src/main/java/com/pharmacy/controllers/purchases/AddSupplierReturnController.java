package com.pharmacy.controllers.purchases;

import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.purchasing.PurchaseDetail;
import com.pharmacy.models.purchasing.SupplierReturn;
import com.pharmacy.models.purchasing.SupplierPayment;
import com.pharmacy.models.security.User;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.services.interfaces.purchasing.SupplierReturnService;
import com.pharmacy.services.interfaces.purchasing.SupplierPaymentService;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.impl.purchasing.PurchaseServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierReturnServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierPaymentServiceImpl;
import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDetailDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierReturnDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierPaymentDAOImpl;
import com.pharmacy.utils.gui.AlertManager;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AddSupplierReturnController implements Initializable {

    @FXML private TextField txtPurchaseId, txtReason;
    @FXML private Label lblSupplierName, lblInvoiceDate, lblTotalInvoice, lblTotalRefund;
    
    @FXML private TableView<ReturnItemWrapper> tvReturnItems;
    @FXML private TableColumn<ReturnItemWrapper, Integer> colIndex, colPurchasedQty, colCurrentStock, colReturnQty;
    @FXML private TableColumn<ReturnItemWrapper, String> colMedicineName, colBatchNumber;
    @FXML private TableColumn<ReturnItemWrapper, BigDecimal> colUnitPrice, colRefundValue;

    private PurchaseService purchaseService;
    private SupplierReturnService returnService;
    private BatchService batchService;
    private MedicineService medicineService;
    private SupplierPaymentService paymentService;

    private Purchase currentPurchase;
    private final ObservableList<ReturnItemWrapper> tableItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        injectServices();
        setupTable();
    }

    private void injectServices() {
        BatchDAOImpl batchDAO = new BatchDAOImpl();
        PurchaseDetailDAOImpl pdDAO = new PurchaseDetailDAOImpl();
        MedicineDAOImpl medDAO = new MedicineDAOImpl();
        SupplierReturnDAOImpl srDAO = new SupplierReturnDAOImpl();

        this.purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), pdDAO);
        this.batchService = new BatchServiceImpl(batchDAO);
        this.medicineService = new MedicineServiceImpl(medDAO);
        this.returnService = new SupplierReturnServiceImpl(srDAO, batchDAO, pdDAO, medDAO);
        this.paymentService = new SupplierPaymentServiceImpl(new SupplierPaymentDAOImpl());
    }

    private void setupTable() {
        tvReturnItems.setEditable(true);

        colIndex.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(tvReturnItems.getItems().indexOf(c.getValue()) + 1));
        colMedicineName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medicineName));
        colBatchNumber.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().batch.getBatch_number()));
        
        colPurchasedQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().remainingReturnable));
        colCurrentStock.setCellValueFactory(c -> {
            int boxesInStock = c.getValue().batch.getQuantity() / c.getValue().conversionFactor;
            return new SimpleObjectProperty<>(boxesInStock);
        });
        
        colUnitPrice.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().detail.getBox_cost()));

        // عمود الكمية المرتجعة قابل للتعديل
        colReturnQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRequestedReturnQty()));
        colReturnQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colReturnQty.setOnEditCommit(event -> {
            ReturnItemWrapper item = event.getRowValue();
            int newVal = event.getNewValue() == null ? 0 : event.getNewValue();
            int boxesInStock = item.batch.getQuantity() / item.conversionFactor;

            if (newVal < 0) {
                AlertManager.showError("خطأ", "الكمية لا يمكن أن تكون سالبة.");
            } else if (newVal > item.remainingReturnable) {
                AlertManager.showError("تجاوز الحد", "الكمية المطلوبة أكبر من المتبقي للإرجاع في الفاتورة (" + item.remainingReturnable + ").");
            } else if (newVal > boxesInStock) {
                AlertManager.showError("نقص مخزون", "لا يوجد رصيد كافٍ في المستودع لهذه الطبخة (المتاح: " + boxesInStock + ").");
            } else {
                item.setRequestedReturnQty(newVal);
                updateSummary();
            }
            tvReturnItems.refresh();
        });

        colRefundValue.setCellValueFactory(c -> {
            BigDecimal price = c.getValue().detail.getBox_cost();
            BigDecimal qty = new BigDecimal(c.getValue().getRequestedReturnQty());
            return new SimpleObjectProperty<>(price.multiply(qty));
        });

        tvReturnItems.setItems(tableItems);
    }

    @FXML
    private void handleFetchPurchase() {
        String idStr = txtPurchaseId.getText().trim();
        if (idStr.isEmpty()) return;

        try {
            int pid = Integer.parseInt(idStr);
            Optional<Purchase> p = purchaseService.getPurchaseById(pid);
            if (p.isPresent()) {
                this.currentPurchase = p.get();
                lblSupplierName.setText("المورد: " + currentPurchase.getSupplier_name()); 
                lblInvoiceDate.setText("التاريخ: " + currentPurchase.getPurchase_date().toLocalDate());
                lblTotalInvoice.setText("الإجمالي: " + currentPurchase.getTotal_cost() + " ل.س");
                
                loadPurchaseItems(pid);
            } else {
                AlertManager.showError("خطأ", "لم يتم العثور على فاتورة بهذا الرقم.");
            }
        } catch (NumberFormatException e) {
            AlertManager.showError("خطأ", "رقم الفاتورة غير صالح.");
        }
    }

    private void loadPurchaseItems(int pid) {
        tableItems.clear();
        List<PurchaseDetail> details = purchaseService.getPurchaseDetailsByPurchaseId(pid);
        List<SupplierReturn> previousReturns = returnService.getReturnsByPurchase(pid);

        for (PurchaseDetail d : details) {
            int alreadyReturned = previousReturns.stream()
                    .filter(r -> r.getBatch_id() == d.getBatch_id())
                    .mapToInt(SupplierReturn::getQuantity_returned).sum();
            
            int remainingReturnable = d.getQuantity_received() - alreadyReturned;

            if (remainingReturnable > 0) { 
                Optional<Batch> b = batchService.getBatchById(d.getBatch_id());
                if (b.isPresent()) {
                    Batch batch = b.get();
                    Medicine med = medicineService.getMedicineById(batch.getMed_id()).orElse(null);
                    
                    int convFactor = (med != null) ? med.getConversion_factor() : 1;
                    String medName = (med != null) ? med.getBrand_name() : "غير معروف";
                    
                    tableItems.add(new ReturnItemWrapper(d, batch, remainingReturnable, convFactor, medName));
                }
            }
        }
        
        if (tableItems.isEmpty()) {
            AlertManager.showWarning("تنبيه", "كافة مواد هذه الفاتورة تم إرجاعها مسبقاً.");
        }
        updateSummary();
    }

    private void updateSummary() {
        BigDecimal total = BigDecimal.ZERO;
        for (ReturnItemWrapper item : tableItems) {
            BigDecimal price = item.detail.getBox_cost();
            BigDecimal qty = new BigDecimal(item.getRequestedReturnQty());
            total = total.add(price.multiply(qty));
        }
        lblTotalRefund.setText(total.toString() + " ل.س");
    }

    @FXML
    private void handleSaveReturn() {
        if (currentPurchase == null) return;

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            AlertManager.showError("خطأ أمان", "يجب تسجيل الدخول لحفظ المرتجعات.");
            return;
        }

        List<SupplierReturn> returnsToProcess = new ArrayList<>();

        for (ReturnItemWrapper item : tableItems) {
            if (item.getRequestedReturnQty() > 0) {
                SupplierReturn sr = new SupplierReturn();
                sr.setPurchase_id(currentPurchase.getPurchase_id());
                sr.setBatch_id(item.batch.getBatch_id());
                sr.setUser_id(currentUser.getUser_id()); 
                sr.setQuantity_returned(item.getRequestedReturnQty());
                
                BigDecimal itemRefundValue = item.detail.getBox_cost().multiply(new BigDecimal(item.getRequestedReturnQty()));
                sr.setTotal_refund_value(itemRefundValue);
                sr.setReason(txtReason.getText());
                sr.setReturn_status("Completed"); 
                
                returnsToProcess.add(sr);
            }
        }

        if (returnsToProcess.isEmpty()) {
            AlertManager.showWarning("تنبيه", "يرجى إدخال كمية إرجاع لسلعة واحدة على الأقل.");
            return;
        }

        // ========================================================
        // 🌟 إضافة رسالة التأكيد (Confirmation Dialog) 🌟
        // ========================================================
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("تأكيد المرتجع");
        confirmAlert.setHeaderText("تحذير: لا يمكن التراجع عن هذه الخطوة!");
        confirmAlert.setContentText("هل أنت متأكد من حفظ هذا المرتجع بقيمة ( " + lblTotalRefund.getText() + " )؟\n\nتنبيه: سيتم خصم الكميات المحددة من رصيد المستودع فوراً.");
        
        ButtonType btnYes = new ButtonType("نعم، احفظ المرتجع", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("إلغاء", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnYes, btnNo);
        
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (!result.isPresent() || result.get() != btnYes) {
            return; // المستخدم تراجع، يتم إيقاف الحفظ
        }
        // ========================================================

        try {
            // 1. معالجة الإرجاع وخصم المخزون
            if (returnService.processMultipleReturns(returnsToProcess)) {
                
                // 2. تحديث الحالة المالية للفاتورة (حسب المعادلة المباشرة)
                updateInvoicePaymentStatusAfterReturn(currentPurchase.getPurchase_id(), currentPurchase.getTotal_cost());

                AlertManager.showSuccess("نجاح", "تم حفظ المرتجع وخصم الكمية من المستودع بنجاح.\nتم تخفيض الرصيد المطلوب للفاتورة.");
                
                // تصفير الواجهة استعداداً لمرتجع جديد
                tableItems.clear();
                txtPurchaseId.clear();
                txtReason.clear();
                lblSupplierName.setText("المورد: ---");
                lblInvoiceDate.setText("تاريخ الشراء: ---");
                lblTotalInvoice.setText("إجمالي الفاتورة: 0 ل.س");
                lblTotalRefund.setText("0.00 ل.س");
                currentPurchase = null;
            }
        } catch (RuntimeException ex) {
            AlertManager.showError("خطأ تنفيذي", ex.getMessage());
        }
    }

    // المعادلة الذهبية المباشرة لتحديث حالة الفاتورة
    private void updateInvoicePaymentStatusAfterReturn(int purchaseId, BigDecimal originalTotalCost) {
        List<SupplierReturn> allReturns = returnService.getReturnsByPurchase(purchaseId);
        BigDecimal totalReturns = allReturns.stream().map(SupplierReturn::getTotal_refund_value).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SupplierPayment> payments = paymentService.getPaymentsByPurchase(purchaseId);
        BigDecimal totalPaid = payments.stream().filter(p -> "Payment".equals(p.getTransaction_type())).map(SupplierPayment::getAmount_paid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRefunds = payments.stream().filter(p -> "Refund".equals(p.getTransaction_type())).map(SupplierPayment::getAmount_paid).reduce(BigDecimal.ZERO, BigDecimal::add);

        // الرصيد = الإجمالي - المرتجعات - المدفوعات + الاستردادات النقدية
        BigDecimal remainingBalance = originalTotalCost.subtract(totalReturns).subtract(totalPaid).add(totalRefunds);

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            purchaseService.updatePaymentStatus(purchaseId, "Paid");
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            purchaseService.updatePaymentStatus(purchaseId, "Partial");
        } else {
            purchaseService.updatePaymentStatus(purchaseId, "Unpaid");
        }
    }

    public static class ReturnItemWrapper {
        PurchaseDetail detail;
        Batch batch;
        int remainingReturnable;
        int conversionFactor;
        String medicineName;
        int requestedReturnQty = 0;

        public ReturnItemWrapper(PurchaseDetail d, Batch b, int rem, int conv, String name) { 
            this.detail = d; this.batch = b; this.remainingReturnable = rem; 
            this.conversionFactor = conv; this.medicineName = name;
        }
        public int getRequestedReturnQty() { return requestedReturnQty; }
        public void setRequestedReturnQty(int qty) { this.requestedReturnQty = qty; }
    }
}