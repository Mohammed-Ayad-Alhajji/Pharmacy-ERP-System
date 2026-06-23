package com.pharmacy.controllers.purchases;

import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.security.Shift;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.services.impl.purchasing.PurchaseServiceImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDAOImpl;
import com.pharmacy.dao.impl.purchasing.PurchaseDetailDAOImpl;
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
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class PurchaseHistoryController implements Initializable {

    // --- FXML Injections ---
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbPaymentStatus;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<Integer> cbRowLimit;

    // Pagination Controls
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label lblPageInfo;

    // Table Columns
    @FXML private TableView<Purchase> tvPurchases;
    @FXML private TableColumn<Purchase, Integer> colIndex; 
    @FXML private TableColumn<Purchase, Integer> colPurchaseId;
    @FXML private TableColumn<Purchase, String> colSupplierInvoice;
    @FXML private TableColumn<Purchase, String> colPurchaseDate; 
    @FXML private TableColumn<Purchase, String> colSupplierName; 
    @FXML private TableColumn<Purchase, BigDecimal> colTotalCost;
    @FXML private TableColumn<Purchase, String> colPaymentStatus;
    @FXML private TableColumn<Purchase, Void> colAction;

    // --- Services & Data ---
    private PurchaseService purchaseService;
    private final ObservableList<Purchase> masterData = FXCollections.observableArrayList();
    private FilteredList<Purchase> filteredData;
    
    // Pagination State
    private int currentPage = 1;

    public PurchaseHistoryController() {}

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        injectServices();
        setupDefaults();
        setupTable();
        loadAllPurchases();
    }

    private void injectServices() {
        this.purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), new PurchaseDetailDAOImpl());
    }

    private void setupDefaults() {
        cbRowLimit.getItems().addAll(10, 20, 50, 100);
        cbRowLimit.setValue(50);
        cbRowLimit.setOnAction(e -> {
            currentPage = 1; // تصفير الصفحة عند تغيير الحد الأقصى
            updatePagination();
        });

        cbPaymentStatus.getItems().addAll("الكل", "مدفوعة نقداً", "آجل (ذمة)");
        cbPaymentStatus.setValue("الكل");
        
        cbPaymentStatus.setOnAction(e -> applyFilters());
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void setupTable() {
        filteredData = new FilteredList<>(masterData, p -> true);

        // الترقيم الديناميكي المتوافق مع الـ Pagination
        colIndex.setCellValueFactory(column -> {
            int limit = cbRowLimit.getValue() != null ? cbRowLimit.getValue() : 50;
            int rowIndex = (currentPage - 1) * limit + tvPurchases.getItems().indexOf(column.getValue()) + 1;
            return new ReadOnlyObjectWrapper<>(rowIndex);
        });

        // ==========================================
        // 1. عرض رقم فاتورتنا بشكل احترافي (INV-000001)
        // ==========================================
        colPurchaseId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPurchase_id()));
        colPurchaseId.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null || id <= 0) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("INV-%06d", id));
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                }
            }
        });

        colSupplierInvoice.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSupplier_invoice_number()));
        colSupplierName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSupplier_name()));
        colTotalCost.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotal_cost()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        colPurchaseDate.setCellValueFactory(cell -> {
            java.time.LocalDateTime date = cell.getValue().getPurchase_date();
            return new SimpleStringProperty(date != null ? date.format(formatter) : "غير محدد");
        });

        // ==========================================
        // 2. تحديث حالة الدفع لتشمل (الدفعة الجزئية) وتلوينها
        // ==========================================
        colPaymentStatus.setCellValueFactory(cell -> {
            String status = cell.getValue().getPayment_status();
            String arabicStatus = "غير معروف";
            if (status != null) {
                switch (status.toLowerCase()) {
                    case "paid": arabicStatus = "مدفوعة نقداً"; break;
                    case "unpaid": arabicStatus = "آجل (ذمة)"; break;
                    case "partial": arabicStatus = "دفعة جزئية"; break; // تمت إضافة هذه الحالة الهامة!
                }
            }
            return new SimpleStringProperty(arabicStatus);
        });

        // تلوين عمود حالة الدفع ليكون أوضح للمستخدم
        colPaymentStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("مدفوعة نقداً")) {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #27ae60; -fx-font-weight: bold;"); // أخضر
                    } else if (item.equals("آجل (ذمة)")) {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // أحمر
                    } else if (item.equals("دفعة جزئية")) {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #e67e22; -fx-font-weight: bold;"); // برتقالي
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        setupActionColumn();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<Purchase, Void>() {
            private final Button btnView = new Button("عرض");
            private final HBox pane = new HBox(10, btnView);

            {
                pane.setAlignment(Pos.CENTER);
                btnView.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");

                btnView.setOnAction(e -> handleViewDetails(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Purchase purchase = getTableView().getItems().get(getIndex());
                    Shift currentShift = SessionManager.getInstance().getCurrentShift();
                    
                    boolean isSameShift = (currentShift != null && currentShift.getShift_id() == purchase.getShift_id());

                    
                    setGraphic(pane);
                }
            }
        });
    }

    /**
     * دالة معمارية لتحديث الصفحات بناءً على البيانات المفلترة
     */
    private void updatePagination() {
        int totalItems = filteredData.size();
        int limit = cbRowLimit.getValue() != null ? cbRowLimit.getValue() : 50;

        int totalPages = (int) Math.ceil((double) totalItems / limit);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * limit;
        int toIndex = Math.min(fromIndex + limit, totalItems);

        // استخراج الجزء المطلوب من الصفحة
        List<Purchase> pageItems = totalItems > 0 ? filteredData.subList(fromIndex, toIndex) : java.util.Collections.emptyList();
        // تغليفها في SortedList لدعم الترتيب بالنقر على رؤوس الأعمدة
        SortedList<Purchase> sortedData = new SortedList<>(FXCollections.observableArrayList(pageItems));
        sortedData.comparatorProperty().bind(tvPurchases.comparatorProperty());
        tvPurchases.setItems(sortedData);

        // تحديث أزرار التنقل
        lblPageInfo.setText("صفحة " + currentPage + " من " + totalPages);
        btnPrevPage.setDisable(currentPage <= 1);
        btnNextPage.setDisable(currentPage >= totalPages);
        
        // إجبار عمود الترقيم على التحديث
        colIndex.setVisible(false);
        colIndex.setVisible(true);
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        updatePagination();
    }

    @FXML
    private void handlePrevPage() {
        currentPage--;
        updatePagination();
    }

    private void applyFilters() {
        filteredData.setPredicate(purchase -> {
            String searchText = txtSearch.getText();
            boolean matchesText = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase().trim();
                boolean matchId = String.valueOf(purchase.getPurchase_id()).contains(lowerCaseFilter);
                boolean matchInvoice = purchase.getSupplier_invoice_number() != null && 
                                       purchase.getSupplier_invoice_number().toLowerCase().contains(lowerCaseFilter);
                boolean matchSupplier = purchase.getSupplier_name() != null && 
                                        purchase.getSupplier_name().toLowerCase().contains(lowerCaseFilter);
                matchesText = matchId || matchInvoice || matchSupplier;
            }

            String selectedStatus = cbPaymentStatus.getValue();
            boolean matchesStatus = true;
            if (selectedStatus != null && !selectedStatus.equals("الكل")) {
                String dbStatus = purchase.getPayment_status() != null ? purchase.getPayment_status() : "";
                switch (selectedStatus) {
                    case "مدفوعة نقداً": matchesStatus = dbStatus.equalsIgnoreCase("Paid"); break;
                    case "آجل (ذمة)": matchesStatus = dbStatus.equalsIgnoreCase("Unpaid"); break;
                    case "دفعة جزئية": matchesStatus = dbStatus.equalsIgnoreCase("Partial"); break;
                }
            }

            return matchesText && matchesStatus;
        });

        // عند أي فلترة جديدة، نعود للصفحة الأولى
        currentPage = 1;
        updatePagination();
    }

    @FXML
    private void loadAllPurchases() {
        txtSearch.clear();
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
        cbPaymentStatus.setValue("الكل");

        Task<List<Purchase>> loadTask = new Task<>() {
            @Override
            protected List<Purchase> call() throws Exception {
                return purchaseService.findAll(); // جلب كل الفواتير وترك التقسيم للـ Pagination
            }
        };

        loadTask.setOnSucceeded(e -> Platform.runLater(() -> {
            masterData.setAll(loadTask.getValue());
            applyFilters(); // هذا سيحدث الـ Pagination تلقائياً
        }));
        loadTask.setOnFailed(e -> Platform.runLater(() -> AlertManager.showError("خطأ", "حدث خطأ أثناء جلب الفواتير.")));
        new Thread(loadTask).start();
    }

    @FXML
    private void handleSearch() {
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        if (start == null || end == null) {
            loadAllPurchases();
            return;
        }

        Task<List<Purchase>> searchTask = new Task<>() {
            @Override
            protected List<Purchase> call() throws Exception {
                return purchaseService.getPurchasesByDateRange(start, end);
            }
        };
        
        searchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            masterData.setAll(searchTask.getValue());
            applyFilters();
        }));
        searchTask.setOnFailed(e -> Platform.runLater(() -> AlertManager.showError("خطأ", "فشل البحث بالتاريخ.")));
        new Thread(searchTask).start();
    }

    private void handleViewDetails(Purchase purchase) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/views/purchases/PurchaseDetailsDialog.fxml"));
            Parent root = loader.load();

            // تمرير الفاتورة المحددة والخدمة إلى كنترولر التفاصيل
            PurchaseDetailsController controller = loader.getController();
            controller.initData(purchase, this.purchaseService);

            Stage stage = new Stage();
            stage.setTitle("تفاصيل الفاتورة رقم: " + purchase.getPurchase_id());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // لقفل الشاشة التي خلفها
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace(); // لتسهيل الـ Debugging في حال حدوث خطأ
            AlertManager.showError("خطأ في النظام", "لم نتمكن من فتح شاشة التفاصيل.");
        }
    }

    private void handleEditPurchase(Purchase purchase) {
        AlertManager.showSuccess("قيد التطوير", "تعديل الفاتورة رقم: " + purchase.getPurchase_id());
    }
}