package com.pharmacy.controllers.purchases;

import com.pharmacy.dao.impl.purchasing.PurchaseDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDetailDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierPaymentDAOImpl;
// استيراد الـ DAOs الخاصة بالأمان والورديات
import com.pharmacy.dao.impl.security.ShiftDAOImpl;
import com.pharmacy.dao.impl.security.UserDAOImpl;

import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.purchasing.Supplier;
import com.pharmacy.models.purchasing.SupplierPayment;
// استيراد موديلات الأمان
import com.pharmacy.models.security.Shift;
import com.pharmacy.models.security.User;

import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.services.interfaces.purchasing.SupplierPaymentService;
import com.pharmacy.services.interfaces.purchasing.SupplierService;
// استيراد خدمات الأمان
import com.pharmacy.services.interfaces.security.ShiftService;
import com.pharmacy.services.interfaces.security.UserService;

import com.pharmacy.services.impl.purchasing.PurchaseServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierPaymentServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierServiceImpl;
// استيراد تطبيقات خدمات الأمان
import com.pharmacy.services.impl.security.ShiftServiceImpl;
import com.pharmacy.services.impl.security.UserServiceImpl;

import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SupplierPaymentsController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbTransactionType;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private ComboBox<Integer> cbRowLimit;
    @FXML private Button btnPrevPage, btnNextPage;
    @FXML private Label lblPageInfo;

    @FXML private TableView<PaymentWrapper> tvPayments;
    @FXML private TableColumn<PaymentWrapper, Integer> colIndex, colPaymentId, colPurchaseId;
    @FXML private TableColumn<PaymentWrapper, String> colSupplierName, colTransactionType, colDate;
    
    // الأعمدة الرقابية
    @FXML private TableColumn<PaymentWrapper, String> colUserName, colShift;
    @FXML private TableColumn<PaymentWrapper, BigDecimal> colAmount;

    // تعريف الخدمات
    private SupplierPaymentService paymentService;
    private SupplierService supplierService;
    private PurchaseService purchaseService; 
    private ShiftService shiftService; // خدمة الورديات
    private UserService userService;   // خدمة المستخدمين
    
    private final ObservableList<PaymentWrapper> masterData = FXCollections.observableArrayList();
    private FilteredList<PaymentWrapper> filteredData;
    private int currentPage = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // حقن جميع الخدمات (DI)
        this.paymentService = new SupplierPaymentServiceImpl(new SupplierPaymentDAOImpl());
        this.supplierService = new SupplierServiceImpl(new SupplierDAOImpl());
        this.purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), new PurchaseDetailDAOImpl());
        this.shiftService = new ShiftServiceImpl(new ShiftDAOImpl());
        this.userService = new UserServiceImpl(new UserDAOImpl());

        setupDefaults();
        setupTable();
        loadInitialData(); 
    }

    private void setupDefaults() {
        cbRowLimit.getItems().addAll(10, 20, 50, 100);
        cbRowLimit.setValue(50);
        cbRowLimit.setOnAction(e -> { currentPage = 1; updatePagination(); });

        cbTransactionType.getItems().addAll("الكل", "دفعة مصروفة (صرف)", "استرداد نقدي (قبض)");
        cbTransactionType.setValue("الكل");
        cbTransactionType.setOnAction(e -> applyFilters());
        
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupTable() {
        filteredData = new FilteredList<>(masterData, p -> true);

        colIndex.setCellValueFactory(c -> {
            int limit = cbRowLimit.getValue() != null ? cbRowLimit.getValue() : 50;
            return new ReadOnlyObjectWrapper<>((currentPage - 1) * limit + tvPayments.getItems().indexOf(c.getValue()) + 1);
        });

        colPaymentId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPayment().getPayment_id()));
        colPaymentId.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) setText(null);
                else setText(String.format("TRX-%06d", id));
            }
        });

        colSupplierName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSupplierName()));
        
        colPurchaseId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPayment().getPurchase_id()));
        colPurchaseId.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty) { 
                    setText(null);
                    setStyle("");
                } else if (id == null || id <= 0) { 
                    setText("دفعة عامة");
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #7f8c8d;");
                } else { 
                    setText(String.format("INV-%06d", id));
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                }
            }
        });

        colTransactionType.setCellValueFactory(c -> new SimpleStringProperty("Refund".equals(c.getValue().getPayment().getTransaction_type()) ? "استرداد نقدي (+)" : "دفعة مصروفة (-)"));
        colTransactionType.setCellFactory(new Callback<>() {
            @Override public TableCell<PaymentWrapper, String> call(TableColumn<PaymentWrapper, String> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); } 
                        else {
                            setText(item);
                            setStyle(item.contains("(+)") ? "-fx-text-fill: #27ae60; -fx-alignment: CENTER; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c; -fx-alignment: CENTER; -fx-font-weight: bold;"); 
                        }
                    }
                };
            }
        });

        colAmount.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPayment().getAmount_paid()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        colDate.setCellValueFactory(c -> c.getValue().getPayment().getPayment_date() != null ? new SimpleStringProperty(c.getValue().getPayment().getPayment_date().format(formatter)) : new SimpleStringProperty("---"));

        // ربط الأعمدة الرقابية
        colShift.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getShiftInfo()));
        colUserName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserName()));
    }

    private void updatePagination() {
        int total = filteredData.size();
        int limit = cbRowLimit.getValue();
        int pages = (int) Math.ceil((double) total / limit);
        if (pages == 0) pages = 1;
        if (currentPage > pages) currentPage = pages;

        int from = (currentPage - 1) * limit;
        int to = Math.min(from + limit, total);

        List<PaymentWrapper> items = total > 0 ? filteredData.subList(from, to) : Collections.emptyList();
        SortedList<PaymentWrapper> sorted = new SortedList<>(FXCollections.observableArrayList(items));
        sorted.comparatorProperty().bind(tvPayments.comparatorProperty());
        tvPayments.setItems(sorted);

        lblPageInfo.setText("صفحة " + currentPage + " من " + pages);
        btnPrevPage.setDisable(currentPage <= 1);
        btnNextPage.setDisable(currentPage >= pages);
        
        colIndex.setVisible(false); colIndex.setVisible(true);
    }

    private void applyFilters() {
        filteredData.setPredicate(p -> {
            String search = txtSearch.getText().toLowerCase().trim();
            
            // تحديث البحث ليشمل اسم الموظف والوردية
            boolean matchSearch = search.isEmpty() || 
                                  p.getSupplierName().toLowerCase().contains(search) || 
                                  (p.getPayment().getPurchase_id() != null && String.valueOf(p.getPayment().getPurchase_id()).contains(search)) ||
                                  p.getUserName().toLowerCase().contains(search) ||
                                  p.getShiftInfo().toLowerCase().contains(search);
            
            String typeFilter = cbTransactionType.getValue();
            boolean matchType = true;
            if ("دفعة مصروفة (صرف)".equals(typeFilter)) matchType = "Payment".equals(p.getPayment().getTransaction_type());
            else if ("استرداد نقدي (قبض)".equals(typeFilter)) matchType = "Refund".equals(p.getPayment().getTransaction_type());

            return matchSearch && matchType;
        });
        
        currentPage = 1; updatePagination();
    }

    @FXML
    public void loadInitialData() {
        dpStartDate.setValue(LocalDate.now().minusMonths(1));
        dpEndDate.setValue(LocalDate.now());
        txtSearch.clear();
        cbTransactionType.setValue("الكل");
        fetchDataFromDatabase(dpStartDate.getValue(), dpEndDate.getValue());
    }

    @FXML
    private void handleSearch() {
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();
        if (start == null || end == null) { AlertManager.showWarning("تنبيه", "يرجى تحديد تاريخ البداية والنهاية."); return; }
        if (start.isAfter(end)) { AlertManager.showError("خطأ", "تاريخ البداية يجب أن يكون قبل النهاية."); return; }
        fetchDataFromDatabase(start, end);
    }

    private void fetchDataFromDatabase(LocalDate start, LocalDate end) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                List<SupplierPayment> payments = paymentService.getPaymentsByDateRange(start, end);
                List<PaymentWrapper> wrappers = new java.util.ArrayList<>();
                
                for (SupplierPayment p : payments) {
                    // 1. جلب اسم المورد
                    String sName = "مورد غير معروف";
                    if (p.getSupplier_id() != null) {
                        Optional<Supplier> sup = supplierService.getSupplierById(p.getSupplier_id());
                        if (sup.isPresent()) sName = sup.get().getName();
                    }
                    
                    // 2. جلب بيانات الوردية والموظف المسؤول من قاعدة البيانات فعلياً
                    String sShift = "غير محدد";
                    String sUser = "غير معروف";
                    
                    if (p.getShift_id() > 0) { // تم إزالة فحص الـ null
                        sShift = "وردية #" + p.getShift_id();
                        
                        // جلب الوردية
                        Optional<Shift> shiftOpt = shiftService.getShiftById(p.getShift_id());
                        if (shiftOpt.isPresent()) {
                            Shift shift = shiftOpt.get();
                            // جلب المستخدم الخاص بهذه الوردية
                            Optional<User> userOpt = userService.getUserById(shift.getUser_id());
                            if (userOpt.isPresent()) {
                                sUser = userOpt.get().getUsername(); // أو getFull_name()
                            }
                        }
                    }
                    
                    wrappers.add(new PaymentWrapper(p, sName, sShift, sUser));
                }
                
                Platform.runLater(() -> { masterData.setAll(wrappers); applyFilters(); });
                return null;
            }
        };
        task.setOnFailed(e -> Platform.runLater(() -> AlertManager.showError("خطأ", "فشل جلب البيانات.")));
        new Thread(task).start();
    }

    @FXML private void handleNextPage() { currentPage++; updatePagination(); }
    @FXML private void handlePrevPage() { currentPage--; updatePagination(); }

    @FXML private void handleAddPayment() { openDialog("Payment"); }
    @FXML private void handleAddRefund() { openDialog("Refund"); }

    private void openDialog(String type) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/views/purchases/AddSupplierPaymentDialog.fxml"));
            Parent root = loader.load();
            AddSupplierPaymentController controller = loader.getController();
            controller.initData(type, this);
            Stage stage = new Stage();
            stage.setTitle("Payment".equals(type) ? "صرف دفعة لمورد" : "استلام استرداد نقدي");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) { 
            e.printStackTrace(); 
            AlertManager.showError("خطأ", "تعذر فتح نافذة تسجيل الحركة."); 
        }
    }

    // =========================================
    // كلاس التغليف 
    // =========================================
    public static class PaymentWrapper {
        private final SupplierPayment payment;
        private final String supplierName;
        private final String shiftInfo;
        private final String userName;

        public PaymentWrapper(SupplierPayment payment, String supplierName, String shiftInfo, String userName) {
            this.payment = payment;
            this.supplierName = supplierName;
            this.shiftInfo = shiftInfo;
            this.userName = userName;
        }

        public SupplierPayment getPayment() { return payment; }
        public String getSupplierName() { return supplierName; }
        public String getShiftInfo() { return shiftInfo; }
        public String getUserName() { return userName; }
    }
}