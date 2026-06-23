package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchMainController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> expiryFilterCombo;

    @FXML private TableView<BatchDTO> batchesTable;
    @FXML private TableColumn<BatchDTO, Integer> indexCol;
    @FXML private TableColumn<BatchDTO, String> medNameCol;
    @FXML private TableColumn<BatchDTO, String> batchNumberCol;
    @FXML private TableColumn<BatchDTO, Integer> quantityCol;
    @FXML private TableColumn<BatchDTO, String> costCol;
    @FXML private TableColumn<BatchDTO, String> mfgDateCol;
    @FXML private TableColumn<BatchDTO, String> expDateCol;
    @FXML private TableColumn<BatchDTO, String> statusCol;
    @FXML private TableColumn<BatchDTO, Void> actionCol;

    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Pagination pagination;

    private BatchService batchService;
    private MedicineService medicineService;

    private ObservableList<BatchDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<BatchDTO> filteredData;
    private Map<Integer, Medicine> medicinesCache;

    @FXML
    public void initialize() {
        batchService = new BatchServiceImpl(new BatchDAOImpl());
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());

        loadMedicinesCache();
        
        setupFilters();
        setupTableColumns();
        setupPaginationControl();
        
        loadAllBatches();

        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void loadMedicinesCache() {
        List<Medicine> allMedicines = medicineService.getAllMedicines();
        medicinesCache = allMedicines.stream()
                .collect(Collectors.toMap(Medicine::getMed_id, med -> med));
    }

    private void setupFilters() {
        expiryFilterCombo.setItems(FXCollections.observableArrayList(
                "الكل", "ساري 🟢", "قريب الانتهاء (90 يوم) 🟠", "منتهي الصلاحية 🔴"
        ));
        expiryFilterCombo.setValue("الكل");
        expiryFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void loadAllBatches() {
        List<Batch> allBatches = batchService.getAllBatches();
        List<BatchDTO> dtoList = allBatches.stream()
                .map(batch -> new BatchDTO(batch, medicinesCache.get(batch.getMed_id())))
                .collect(Collectors.toList());

        masterData.setAll(dtoList);
        filteredData = new FilteredList<>(masterData, p -> true);
        applyFilters();
    }

    private void setupTableColumns() {
        indexCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
        indexCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                } else {
                    int currentPage = pagination.getCurrentPageIndex();
                    int rowsPerPage = rowsPerPageCombo.getValue();
                    setText(String.valueOf((currentPage * rowsPerPage) + getIndex() + 1));
                }
            }
        });

        medNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicineName()));
        batchNumberCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBatch().getBatch_number()));
        quantityCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getBatch().getQuantity()));
        costCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBatch().getBuy_box_cost() + " ل.س"));
        mfgDateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBatch().getMfg_date().toString()));
        expDateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBatch().getExp_date().toString()));

        // ==========================================
        // اللمسة الاحترافية: تلوين عمود الحالة فقط
        // ==========================================
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatusText()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    BatchDTO dto = (BatchDTO) getTableRow().getItem();
                    long days = dto.getDaysToExpiry();
                    
                    // تلوين النص مع خلفية خفيفة جداً لجمالية أكثر
                    if (days < 0) {
                        setStyle("-fx-text-fill: #e11d48; -fx-font-weight: bold; -fx-background-color: #ffe4e6;");
                    } else if (days <= 90) {
                        setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold; -fx-background-color: #fef3c7;");
                    } else {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-background-color: #dcfce7;");
                    }
                }
            }
        });

        // ==========================================
        // عمود الإجراءات (تنشيط / إيقاف التشغيلة)
        // ==========================================
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button toggleBtn = new Button();
            {
                toggleBtn.setOnAction(e -> {
                    BatchDTO dto = getTableView().getItems().get(getIndex());
                    handleToggleBatch(dto.getBatch());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    BatchDTO dto = (BatchDTO) getTableRow().getItem();
                    if (dto.getBatch().getIs_active() == 1) {
                        toggleBtn.setText("إيقاف 🛑");
                        toggleBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
                    } else {
                        toggleBtn.setText("تنشيط ♻️");
                        toggleBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
                    }
                    setGraphic(toggleBtn);
                }
            }
        });
    }

    private void handleToggleBatch(Batch batch) {
        String action = batch.getIs_active() == 1 ? "إيقاف" : "تنشيط";
        if (AlertManager.showConfirmation("تأكيد", "هل تريد " + action + " التشغيلة رقم: " + batch.getBatch_number() + "؟")) {
            batch.setIs_active(batch.getIs_active() == 1 ? 0 : 1);
            if (batchService.updateBatch(batch)) {
                loadAllBatches(); // لتحديث الجدول
            } else {
                AlertManager.showError("خطأ", "حدث خطأ أثناء تعديل حالة التشغيلة.");
            }
        }
    }

    private void applyFilters() {
        if (filteredData == null) return;
        
        filteredData.setPredicate(dto -> {
            // فلتر البحث النصي
            String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            boolean matchesSearch = search.isEmpty() || 
                    dto.getMedicineName().toLowerCase().contains(search) ||
                    (dto.getMedicine() != null && dto.getMedicine().getBarcode() != null && dto.getMedicine().getBarcode().contains(search)) ||
                    dto.getBatch().getBatch_number().toLowerCase().contains(search);
            
            if (!matchesSearch) return false;

            // فلتر القائمة المنسدلة للصلاحية
            String selectedFilter = expiryFilterCombo.getValue();
            if ("ساري 🟢".equals(selectedFilter)) {
                if (dto.getDaysToExpiry() <= 90) return false;
            } else if ("قريب الانتهاء (90 يوم) 🟠".equals(selectedFilter)) {
                if (dto.getDaysToExpiry() < 0 || dto.getDaysToExpiry() > 90) return false;
            } else if ("منتهي الصلاحية 🔴".equals(selectedFilter)) {
                if (dto.getDaysToExpiry() >= 0) return false;
            }

            return true;
        });

        updatePagination();
    }

    private void setupPaginationControl() {
        rowsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        rowsPerPageCombo.setValue(20);
        rowsPerPageCombo.valueProperty().addListener((obs, old, newVal) -> updatePagination());
        pagination.currentPageIndexProperty().addListener((obs, old, newVal) -> updateTableData(newVal.intValue()));
    }

    private void updatePagination() {
        int rowsPerPage = rowsPerPageCombo.getValue();
        int pageCount = (int) Math.ceil((double) filteredData.size() / rowsPerPage);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        updateTableData(0);
    }

    private void updateTableData(int pageIndex) {
        int rowsPerPage = rowsPerPageCombo.getValue();
        int from = pageIndex * rowsPerPage;
        int to = Math.min(from + rowsPerPage, filteredData.size());
        batchesTable.setItems(FXCollections.observableArrayList(from < to ? filteredData.subList(from, to) : FXCollections.observableArrayList()));
    }

    // ==========================================
    // كلاس داخلي (DTO)
    // ==========================================
    public static class BatchDTO {
        private final Batch batch;
        private final Medicine medicine;
        private final long daysToExpiry;

        public BatchDTO(Batch batch, Medicine medicine) {
            this.batch = batch;
            this.medicine = medicine;
            this.daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExp_date());
        }

        public Batch getBatch() { return batch; }
        public Medicine getMedicine() { return medicine; }
        public long getDaysToExpiry() { return daysToExpiry; }

        public String getMedicineName() {
            return medicine != null ? medicine.getBrand_name() : "غير معروف (ID: " + batch.getMed_id() + ")";
        }

        public String getStatusText() {
            if (daysToExpiry < 0) return "منتهي الصلاحية 🛑";
            if (daysToExpiry == 0) return "ينتهي اليوم ⚠️";
            if (daysToExpiry <= 90) return "متبقي " + daysToExpiry + " يوم 🟠";
            return "ساري 🟢";
        }
    }
}