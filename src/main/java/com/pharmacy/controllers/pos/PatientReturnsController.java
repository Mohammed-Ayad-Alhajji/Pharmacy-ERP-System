package com.pharmacy.controllers.pos;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.pos.InsuranceCompanyDAOImpl;
import com.pharmacy.dao.impl.pos.LocalCustomerDAOImpl;
import com.pharmacy.dao.impl.pos.PatientReturnDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDetailDAOImpl;
import com.pharmacy.models.pos.PatientReturn;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.impl.pos.InsuranceCompanyServiceImpl;
import com.pharmacy.services.impl.pos.LocalCustomerServiceImpl;
import com.pharmacy.services.impl.pos.PatientReturnServiceImpl;
import com.pharmacy.services.impl.pos.SaleServiceImpl;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.PatientReturnService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PatientReturnsController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private DatePicker dpStart, dpEnd;
    
    @FXML private TableView<ReturnWrapper> tvReturns;
    @FXML private TableColumn<ReturnWrapper, Integer> colIndex;
    @FXML private TableColumn<ReturnWrapper, String> colSaleId, colCustomer, colMedicineBatch;
    @FXML private TableColumn<ReturnWrapper, Integer> colQtyReturned;
    @FXML private TableColumn<ReturnWrapper, String> colRefundAmount, colReturnDate, colPharmacist, colReason;

    // عناصر الـ Pagination
    @FXML private ComboBox<Integer> cbRowsPerPage;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label lblPageInfo;

    private List<ReturnWrapper> masterReturnsList = new ArrayList<>();
    private List<ReturnWrapper> currentFilteredList = new ArrayList<>();
    
    private int currentPage = 1;
    private int rowsPerPage = 50;

    private SaleService saleService;
    private MedicineService medicineService;
    private BatchService batchService;
    private LocalCustomerService customerService;
    private InsuranceCompanyService insuranceService;
    private PatientReturnService returnService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initServices();
        setupTable();
        setupPaginationControls();
        
        // ربط البحث اللحظي بنظام الصفحات
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1; 
            updatePagination();
        });
        
        loadInitialData();
    }

    private void initServices() {
        this.saleService = new SaleServiceImpl(new SaleDAOImpl(), new SaleDetailDAOImpl());
        this.medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        this.batchService = new BatchServiceImpl(new BatchDAOImpl());
        this.customerService = new LocalCustomerServiceImpl(new LocalCustomerDAOImpl());
        this.insuranceService = new InsuranceCompanyServiceImpl(new InsuranceCompanyDAOImpl());
        this.returnService = new PatientReturnServiceImpl(new PatientReturnDAOImpl());
    }

    private void setupPaginationControls() {
        cbRowsPerPage.getItems().addAll(25, 50, 100, 500);
        cbRowsPerPage.setValue(50);
        cbRowsPerPage.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                rowsPerPage = newVal;
                currentPage = 1;
                updatePagination();
            }
        });
    }

    private void setupTable() {
        colIndex.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tvReturns.getItems().indexOf(column.getValue()) + 1 + ((currentPage - 1) * rowsPerPage)));
        colSaleId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().saleId));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName));
        colMedicineBatch.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medicineName + " (Batch: " + c.getValue().batchNumber + ")"));
        colQtyReturned.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().quantityReturned));
        colRefundAmount.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().cashRefund)));
        colReturnDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().returnDate));
        colPharmacist.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().pharmacistName));
        colReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason != null ? c.getValue().reason : "---"));
    }

    @FXML
    public void loadInitialData() {
        dpStart.setValue(LocalDate.now().minusDays(30)); 
        dpEnd.setValue(LocalDate.now());
        txtSearch.clear();
        handleSearch();
    }

    @FXML
    private void handleSearch() {
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();
        if (start == null || end == null) return;
        LocalDate adjustedEnd = end.plusDays(1);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<PatientReturn> returns = returnService.getReturnsByDateRange(start, adjustedEnd);

                    // الترتيب من الأحدث للأقدم بناءً على تاريخ المرتجع
                    returns.sort((r1, r2) -> {
                        if (r1.getReturn_date() == null && r2.getReturn_date() == null) return 0;
                        if (r1.getReturn_date() == null) return 1;
                        if (r2.getReturn_date() == null) return -1;
                        return r2.getReturn_date().compareTo(r1.getReturn_date()); // Descending
                    });

                    List<ReturnWrapper> wrappers = new ArrayList<>();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");

                    for (PatientReturn pr : returns) {
                        String rDate = pr.getReturn_date() != null ? pr.getReturn_date().format(formatter) : "";
                        // ملاحظة: يمكنك لاحقاً استخدام JOIN لجلب الأسماء الحقيقية بدلاً من وضع Detail ID
                        wrappers.add(new ReturnWrapper(
                                "Detail ID: " + pr.getDetail_id(), 
                                "زبون عام", 
                                "دواء مباع", 
                                "N/A", 
                                pr.getQuantity_returned(), 
                                pr.getPatient_cash_refund(), 
                                rDate, 
                                "المستخدم الحالي", 
                                pr.getReason()
                        ));
                    }

                    Platform.runLater(() -> {
                        masterReturnsList.clear();
                        masterReturnsList.addAll(wrappers);
                        currentPage = 1; // العودة للصفحة الأولى بعد بحث جديد
                        updatePagination();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> AlertManager.showError("خطأ", "فشل جلب سجل المرتجعات:\n" + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    // الدالة المركزية لمعالجة البحث والتقسيم معاً
    private void updatePagination() {
        String search = txtSearch.getText().toLowerCase().trim();
        currentFilteredList.clear();

        // 1. تطبيق الفلترة (البحث)
        for (ReturnWrapper rw : masterReturnsList) {
            if (search.isEmpty() || rw.saleId.toLowerCase().contains(search) || 
                rw.customerName.toLowerCase().contains(search) || 
                rw.medicineName.toLowerCase().contains(search)) {
                currentFilteredList.add(rw);
            }
        }

        // 2. حساب عدد الصفحات
        int totalItems = currentFilteredList.size();
        int totalPages = (int) Math.ceil((double) totalItems / rowsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        // 3. جلب الأسطر المخصصة للصفحة الحالية فقط
        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        // 4. تحديث الجدول
        tvReturns.getItems().setAll(currentFilteredList.subList(fromIndex, toIndex));

        // 5. تحديث معلومات الواجهة (أزرار التقليب والنص)
        lblPageInfo.setText("صفحة " + currentPage + " من " + totalPages);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
        
        // تحديث الترقيم
        colIndex.setVisible(false);
        colIndex.setVisible(true);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) currentFilteredList.size() / rowsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    @FXML
    private void handleCreateNewReturn(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/views/pos/CreatePatientReturnView.fxml"));
            Parent root = loader.load();
            
            CreatePatientReturnController controller = loader.getController();
            controller.initServices(saleService, medicineService, batchService, customerService, returnService, insuranceService);

            Stage stage = new Stage();
            stage.setTitle("إنشاء مرتجع مريض جديد");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); 
            
            stage.showAndWait(); 
            handleSearch(); // جلب البيانات المحدثة (التي ستظهر في أول صفحة لأنها الأحدث)

        } catch (Exception e) {
            e.printStackTrace();
            AlertManager.showError("خطأ", "تعذر فتح نافذة إرجاع المبيعات.");
        }
    }

    private String formatMoney(BigDecimal amount) {
        return amount != null ? String.format("%,.2f ل.س", amount) : "0.00 ل.س";
    }

    public static class ReturnWrapper {
        String saleId, customerName, medicineName, batchNumber, returnDate, pharmacistName, reason;
        int quantityReturned;
        BigDecimal cashRefund;

        public ReturnWrapper(String saleId, String customerName, String medicineName, String batchNumber, int quantityReturned, BigDecimal cashRefund, String returnDate, String pharmacistName, String reason) {
            this.saleId = saleId; this.customerName = customerName; this.medicineName = medicineName;
            this.batchNumber = batchNumber; this.quantityReturned = quantityReturned; this.cashRefund = cashRefund;
            this.returnDate = returnDate; this.pharmacistName = pharmacistName; this.reason = reason;
        }
    }
}