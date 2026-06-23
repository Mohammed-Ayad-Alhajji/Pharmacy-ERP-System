package com.pharmacy.controllers.purchases;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDetailDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierPaymentDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierReturnDAOImpl;
import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.purchasing.PurchaseDetail;
import com.pharmacy.models.purchasing.SupplierPayment;
import com.pharmacy.models.purchasing.SupplierReturn;
import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.services.interfaces.purchasing.SupplierPaymentService;
import com.pharmacy.services.interfaces.purchasing.SupplierReturnService;
import com.pharmacy.services.impl.purchasing.SupplierPaymentServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierReturnServiceImpl;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.export.ExportUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class PurchaseDetailsController implements Initializable {

    @FXML private Label lblPurchaseId, lblSupplierInvoice, lblSupplierName, lblPurchaseDate;
    @FXML private Label lblTotalCost, lblTotalReturns, lblNetTotal, lblPaymentStatus;
    
    // التسميات الجديدة للمدفوعات والرصيد
    @FXML private Label lblTotalPaid, lblRemainingBalance; 
    @FXML private TableView<InvoiceItemWrapper> tvDetails;
    @FXML private TableColumn<InvoiceItemWrapper, Integer> colIndex, colBatchId, colOriginalQty, colReturnedQty, colNetQty, colBonus;
    @FXML private TableColumn<InvoiceItemWrapper, BigDecimal> colBoxCost, colNetSubTotal;

    private PurchaseService purchaseService;
    private SupplierReturnService returnService;
    private SupplierPaymentService paymentService; // الخدمة الجديدة
    
    private final ObservableList<InvoiceItemWrapper> detailsData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // تهيئة خدمة المرتجعات والدفعات محلياً
        this.returnService = new SupplierReturnServiceImpl(
            new SupplierReturnDAOImpl(), new BatchDAOImpl(), 
            new PurchaseDetailDAOImpl(), new MedicineDAOImpl()
        );
        this.paymentService = new SupplierPaymentServiceImpl(new SupplierPaymentDAOImpl());
        
        setupTable();
    }

    private void setupTable() {
        colIndex.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(tvDetails.getItems().indexOf(c.getValue()) + 1));
        
        colBatchId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().detail.getBatch_id()));
        colOriginalQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().detail.getQuantity_received()));
        colReturnedQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().returnedQty));
        colNetQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNetQty()));
        colBonus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().detail.getBonus_quantity()));
        colBoxCost.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().detail.getBox_cost()));

        // المجموع الصافي = (الكمية الصافية * سعر العلبة)
        colNetSubTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNetSubTotal()));

        tvDetails.setItems(detailsData);
    }

    public void initData(Purchase purchase, PurchaseService purchaseService) {
        this.purchaseService = purchaseService;

        lblPurchaseId.setText(String.valueOf(purchase.getPurchase_id()));
        lblSupplierInvoice.setText(purchase.getSupplier_invoice_number() != null ? purchase.getSupplier_invoice_number() : "---");
        lblSupplierName.setText(purchase.getSupplier_name());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        lblPurchaseDate.setText(purchase.getPurchase_date().format(formatter));
        lblTotalCost.setText(purchase.getTotal_cost().toString() + " ل.س");

        String status = purchase.getPayment_status();
        if ("Paid".equalsIgnoreCase(status)) lblPaymentStatus.setText("مدفوعة نقداً");
        else if ("Unpaid".equalsIgnoreCase(status)) lblPaymentStatus.setText("آجل (ذمة)");
        else lblPaymentStatus.setText(status);

        loadInvoiceData(purchase.getPurchase_id());
    }

    private void loadInvoiceData(int purchaseId) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                // 1. جلب البيانات الأساسية
                List<PurchaseDetail> details = purchaseService.getPurchaseDetailsByPurchaseId(purchaseId);
                List<SupplierReturn> returns = returnService.getReturnsByPurchase(purchaseId);
                List<SupplierPayment> allTransactions = paymentService.getPaymentsByPurchase(purchaseId);

                // 2. الحساب المالي الدقيق للمدفوعات (الفرق بين الصرف والاسترداد)
                BigDecimal totalPaymentsOnly = allTransactions.stream()
                        .filter(p -> "Payment".equals(p.getTransaction_type()))
                        .map(SupplierPayment::getAmount_paid)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalRefundsOnly = allTransactions.stream()
                        .filter(p -> "Refund".equals(p.getTransaction_type()))
                        .map(SupplierPayment::getAmount_paid)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // المدفوع الفعلي الذي خرج من الصيدلية ولم يَعُد
                BigDecimal netPaidValue = totalPaymentsOnly.subtract(totalRefundsOnly);

                // 3. دمج بيانات الأصناف وحساب المرتجعات السلعية
                List<InvoiceItemWrapper> wrappers = new java.util.ArrayList<>();
                BigDecimal totalReturnsValue = BigDecimal.ZERO;
                BigDecimal netInvoiceTotal = BigDecimal.ZERO;

                for (PurchaseDetail d : details) {
                    int returnedQty = returns.stream()
                            .filter(r -> r.getBatch_id() == d.getBatch_id())
                            .mapToInt(SupplierReturn::getQuantity_returned)
                            .sum();
                    
                    InvoiceItemWrapper wrapper = new InvoiceItemWrapper(d, returnedQty);
                    wrappers.add(wrapper);

                    // إجمالي قيمة الأدوية المرجعة (تخفيض من أصل الفاتورة)
                    totalReturnsValue = totalReturnsValue.add(
                            wrapper.detail.getBox_cost().multiply(new BigDecimal(returnedQty))
                    );
                    netInvoiceTotal = netInvoiceTotal.add(wrapper.getNetSubTotal());
                }

                // 4. المعادلة النهائية: الرصيد = (قيمة البضاعة الباقية عندنا) - (صافي ما دفعناه للمورد)
                BigDecimal remainingBalance = netInvoiceTotal.subtract(netPaidValue);

                // تجهيز المتغيرات النهائية للواجهة
                final BigDecimal finalTotalReturns = totalReturnsValue;
                final BigDecimal finalNetTotal = netInvoiceTotal;
                final BigDecimal finalNetPaid = netPaidValue;
                final BigDecimal finalRemaining = remainingBalance;

                Platform.runLater(() -> {
                    detailsData.setAll(wrappers);
                    lblTotalReturns.setText(finalTotalReturns.toString() + " ل.س");
                    lblNetTotal.setText(finalNetTotal.toString() + " ل.س");
                    lblTotalPaid.setText(finalNetPaid.toString() + " ل.س");

                    // التنسيق البصري للرصيد (مدين أم دائن)
                    if (finalRemaining.compareTo(BigDecimal.ZERO) < 0) {
                        // المورد مدين لنا (دفعنا أكثر مما نملك بضاعة بسبب المرتجعات)
                        lblRemainingBalance.setText(finalRemaining.abs().toString() + " ل.س (رصيد لصالحنا)");
                        lblRemainingBalance.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else if (finalRemaining.compareTo(BigDecimal.ZERO) > 0) {
                        // نحن مدينون للمورد (ذمة متبقية)
                        lblRemainingBalance.setText(finalRemaining.toString() + " ل.س (رصيد علينا)");
                        lblRemainingBalance.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else {
                        lblRemainingBalance.setText("0.00 ل.س (مسددة بالكامل)");
                        lblRemainingBalance.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 16px;");
                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    // ==========================================
    // دوال التصدير (Export)
    // ==========================================

    @FXML 
    private void handleExportPDF() { 
        if (tvDetails.getItems().isEmpty()) {
            AlertManager.showError("تنبيه", "لا توجد بيانات في الفاتورة لتصديرها.");
            return;
        }
        // سحب رقم الفاتورة من الواجهة لوضعه في العنوان
        String title = "البيان التفصيلي لفاتورة المشتريات رقم: " + lblPurchaseId.getText();
        ExportUtils.exportToPDF(tvDetails, title);
    }

    @FXML 
    private void handleExportExcel() { 
        if (tvDetails.getItems().isEmpty()) {
            AlertManager.showError("تنبيه", "لا توجد بيانات في الفاتورة لتصديرها.");
            return;
        }
        // سحب رقم الفاتورة لتسمية ملف الإكسل
        String fileName = "تفاصيل_مشتريات_رقم_" + lblPurchaseId.getText();
        ExportUtils.exportToExcel(tvDetails, fileName);
    }
    @FXML private void handleClose() { ((Stage) tvDetails.getScene().getWindow()).close(); }

    /**
     * كلاس تغليف داخلي لدمج سطر الشراء مع المرتجع
     */
    public static class InvoiceItemWrapper {
        PurchaseDetail detail;
        int returnedQty;

        public InvoiceItemWrapper(PurchaseDetail detail, int returnedQty) {
            this.detail = detail;
            this.returnedQty = returnedQty;
        }

        public int getNetQty() {
            return detail.getQuantity_received() - returnedQty;
        }

        public BigDecimal getNetSubTotal() {
            BigDecimal netQtyBd = new BigDecimal(getNetQty());
            return detail.getBox_cost().multiply(netQtyBd);
        }
    }
}