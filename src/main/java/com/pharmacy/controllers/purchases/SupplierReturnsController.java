package com.pharmacy.controllers.purchases;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDetailDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierDAOImpl;
import com.pharmacy.dao.impl.purchasing.SupplierReturnDAOImpl;
import com.pharmacy.dao.impl.security.UserDAOImpl;

import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.purchasing.Supplier;
import com.pharmacy.models.purchasing.SupplierReturn;
import com.pharmacy.models.security.User;

import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.services.interfaces.purchasing.SupplierReturnService;
import com.pharmacy.services.interfaces.purchasing.SupplierService;
import com.pharmacy.services.interfaces.security.UserService;

import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.impl.purchasing.PurchaseServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierReturnServiceImpl;
import com.pharmacy.services.impl.purchasing.SupplierServiceImpl;
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

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SupplierReturnsController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private ComboBox<Integer> cbRowLimit;
    @FXML private Button btnPrevPage, btnNextPage;
    @FXML private Label lblPageInfo;

    @FXML private TableView<ReturnWrapper> tvReturns;
    @FXML private TableColumn<ReturnWrapper, Integer> colIndex, colQuantity;
    @FXML private TableColumn<ReturnWrapper, String> colPurchaseId, colSupplierName, colMedicineInfo, colDate, colUserName, colReason;
    @FXML private TableColumn<ReturnWrapper, BigDecimal> colRefundValue;

    // حقن الخدمات المطلوبة
    private SupplierReturnService returnService;
    private PurchaseService purchaseService;
    private SupplierService supplierService;
    private BatchService batchService;
    private MedicineService medicineService;
    private UserService userService;

    private final ObservableList<ReturnWrapper> masterData = FXCollections.observableArrayList();
    private FilteredList<ReturnWrapper> filteredData;
    private int currentPage = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // تهيئة الخدمات
        BatchDAOImpl batchDAO = new BatchDAOImpl();
        PurchaseDetailDAOImpl pdDAO = new PurchaseDetailDAOImpl();
        MedicineDAOImpl medDAO = new MedicineDAOImpl();
        
        this.returnService = new SupplierReturnServiceImpl(new SupplierReturnDAOImpl(), batchDAO, pdDAO, medDAO);
        this.purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), pdDAO);
        this.supplierService = new SupplierServiceImpl(new SupplierDAOImpl());
        this.batchService = new BatchServiceImpl(batchDAO);
        this.medicineService = new MedicineServiceImpl(medDAO);
        this.userService = new UserServiceImpl(new UserDAOImpl());

        setupDefaults();
        setupTable();
        loadInitialData();
    }

    private void setupDefaults() {
        cbRowLimit.getItems().addAll(10, 20, 50, 100);
        cbRowLimit.setValue(50);
        cbRowLimit.setOnAction(e -> { currentPage = 1; updatePagination(); });

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupTable() {
        filteredData = new FilteredList<>(masterData, p -> true);

        colIndex.setCellValueFactory(c -> {
            int limit = cbRowLimit.getValue() != null ? cbRowLimit.getValue() : 50;
            return new ReadOnlyObjectWrapper<>((currentPage - 1) * limit + tvReturns.getItems().indexOf(c.getValue()) + 1);
        });

        colPurchaseId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInvoiceFormatted()));
        colSupplierName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSupplierName()));
        colMedicineInfo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicineInfo()));
        
        colQuantity.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSupplierReturn().getQuantity_returned()));
        colRefundValue.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSupplierReturn().getTotal_refund_value()));
        
        colUserName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserName()));
        
        colReason.setCellValueFactory(c -> {
            String reason = c.getValue().getSupplierReturn().getReason();
            return new SimpleStringProperty((reason != null && !reason.trim().isEmpty()) ? reason : "---");
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        colDate.setCellValueFactory(c -> {
            // بافتراض وجود حقل return_date، وإلا نضع قيمة افتراضية للوقت الحالي مؤقتاً إذا لم يكن مدعوماً
            if (c.getValue().getSupplierReturn().getReturn_date() != null) {
                return new SimpleStringProperty(c.getValue().getSupplierReturn().getReturn_date().format(formatter));
            }
            return new SimpleStringProperty("---");
        });
    }

    private void updatePagination() {
        int total = filteredData.size();
        int limit = cbRowLimit.getValue();
        int pages = (int) Math.ceil((double) total / limit);
        if (pages == 0) pages = 1;
        if (currentPage > pages) currentPage = pages;

        int from = (currentPage - 1) * limit;
        int to = Math.min(from + limit, total);

        List<ReturnWrapper> items = total > 0 ? filteredData.subList(from, to) : Collections.emptyList();
        SortedList<ReturnWrapper> sorted = new SortedList<>(FXCollections.observableArrayList(items));
        sorted.comparatorProperty().bind(tvReturns.comparatorProperty());
        tvReturns.setItems(sorted);

        lblPageInfo.setText("صفحة " + currentPage + " من " + pages);
        btnPrevPage.setDisable(currentPage <= 1);
        btnNextPage.setDisable(currentPage >= pages);
        
        colIndex.setVisible(false); colIndex.setVisible(true);
    }

    private void applyFilters() {
        filteredData.setPredicate(p -> {
            String search = txtSearch.getText().toLowerCase().trim();
            
            return search.isEmpty() || 
                   p.getSupplierName().toLowerCase().contains(search) || 
                   p.getInvoiceFormatted().toLowerCase().contains(search) ||
                   p.getMedicineInfo().toLowerCase().contains(search) ||
                   p.getUserName().toLowerCase().contains(search) ||
                   (p.getSupplierReturn().getReason() != null && p.getSupplierReturn().getReason().toLowerCase().contains(search));
        });
        
        currentPage = 1; updatePagination();
    }

    @FXML
    public void loadInitialData() {
        dpStartDate.setValue(LocalDate.now().minusMonths(1));
        dpEndDate.setValue(LocalDate.now());
        txtSearch.clear();
        fetchDataFromDatabase();
    }

    @FXML
    private void handleSearch() {
        if (dpStartDate.getValue() == null || dpEndDate.getValue() == null) { 
            AlertManager.showWarning("تنبيه", "يرجى تحديد تاريخ البداية والنهاية."); return; 
        }
        if (dpStartDate.getValue().isAfter(dpEndDate.getValue())) { 
            AlertManager.showError("خطأ", "تاريخ البداية يجب أن يكون قبل النهاية."); return; 
        }
        fetchDataFromDatabase();
    }

    private void fetchDataFromDatabase() {
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        if (start == null || end == null) {
            AlertManager.showWarning("تنبيه", "يرجى تحديد تاريخ البداية والنهاية.");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // استخدام الدالة المتخصصة من الـ Service (التي تستدعي findByDateRange في الـ DAO)
                // تأكد أن الـ Service لديك يمرر هذه القيم للـ DAO
                List<SupplierReturn> returns = returnService.getReturnsByDateRange(start, end);

                List<ReturnWrapper> wrappers = new java.util.ArrayList<>();
                
                for (SupplierReturn r : returns) {
                    String sSupplierName = "مورد غير معروف";
                    String sMedicineInfo = "دواء غير معروف";
                    String sUserName = "مستخدم غير معروف";

                    // 1. جلب بيانات الفاتورة والمورد
                    if (r.getPurchase_id() > 0) {
                        Optional<Purchase> pur = purchaseService.getPurchaseById(r.getPurchase_id());
                        if (pur.isPresent() && pur.get().getSupplier_id() > 0) {
                            Optional<Supplier> sup = supplierService.getSupplierById(pur.get().getSupplier_id());
                            if (sup.isPresent()) sSupplierName = sup.get().getName();
                        }
                    }

                    // 2. جلب بيانات الدواء والطبخة
                    if (r.getBatch_id() > 0) {
                        Optional<Batch> batchOpt = batchService.getBatchById(r.getBatch_id());
                        if (batchOpt.isPresent()) {
                            Batch batch = batchOpt.get();
                            Optional<Medicine> medOpt = medicineService.getMedicineById(batch.getMed_id());
                            if (medOpt.isPresent()) {
                                sMedicineInfo = medOpt.get().getBrand_name() + " (Batch: " + batch.getBatch_number() + ")";
                            }
                        }
                    }

                    // 3. جلب بيانات المستخدم المسؤول عن المرتجع
                    if (r.getUser_id() > 0) {
                        Optional<User> userOpt = userService.getUserById(r.getUser_id());
                        if (userOpt.isPresent()) {
                            sUserName = userOpt.get().getUsername();
                        }
                    }
                    
                    wrappers.add(new ReturnWrapper(r, sSupplierName, sMedicineInfo, sUserName));
                }
                
                // عكس الترتيب ليظهر الأحدث أولاً
                Collections.reverse(wrappers);
                
                Platform.runLater(() -> { 
                    masterData.setAll(wrappers); 
                    applyFilters(); 
                });
                return null;
            }
        };
        
        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            Platform.runLater(() -> AlertManager.showError("خطأ", "فشل جلب بيانات المرتجعات من قاعدة البيانات."));
        });
        
        new Thread(task).start();
    }

    @FXML 
    private void handleNextPage() { currentPage++; updatePagination(); }
    
    @FXML 
    private void handlePrevPage() { currentPage--; updatePagination(); }

    @FXML 
    private void handleAddReturn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/views/purchases/AddSupplierReturnDialog.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("إنشاء مرتجع جديد");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            
            // تحديث الجدول بعد إغلاق شاشة الإضافة
            loadInitialData();
            
        } catch (Exception e) { 
            e.printStackTrace(); 
            AlertManager.showError("خطأ", "تعذر فتح نافذة المرتجعات."); 
        }
    }

    // =========================================
    // كلاس التغليف (Wrapper)
    // =========================================
    public static class ReturnWrapper {
        private final SupplierReturn supplierReturn;
        private final String supplierName;
        private final String medicineInfo;
        private final String userName;

        public ReturnWrapper(SupplierReturn supplierReturn, String supplierName, String medicineInfo, String userName) {
            this.supplierReturn = supplierReturn;
            this.supplierName = supplierName;
            this.medicineInfo = medicineInfo;
            this.userName = userName;
        }

        public SupplierReturn getSupplierReturn() { return supplierReturn; }
        public String getSupplierName() { return supplierName; }
        public String getMedicineInfo() { return medicineInfo; }
        public String getUserName() { return userName; }
        
        public String getInvoiceFormatted() {
            return supplierReturn.getPurchase_id() > 0 ? String.format("INV-%06d", supplierReturn.getPurchase_id()) : "---";
        }
    }
}