package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.CategoryDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Category;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.CategoryServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.CategoryService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MedicineMainController {

    @FXML private TextField searchField;
    @FXML private MenuButton categoryFilterMenu;
    @FXML private CheckBox showInactiveCheckBox;
    @FXML private CheckBox showLowStockCheckBox; // الفلتر الجديد
    
    @FXML private TableView<MedicineDTO> medicinesTable; // تعديل النوع إلى DTO
    @FXML private TableColumn<MedicineDTO, Integer> indexCol;
    @FXML private TableColumn<MedicineDTO, String> barcodeCol;
    @FXML private TableColumn<MedicineDTO, String> brandNameCol;
    @FXML private TableColumn<MedicineDTO, String> genericNameCol;
    @FXML private TableColumn<MedicineDTO, String> categoryCol;
    @FXML private TableColumn<MedicineDTO, Integer> totalStockCol; // العمود الجديد
    @FXML private TableColumn<MedicineDTO, BigDecimal> boxPriceCol;
    @FXML private TableColumn<MedicineDTO, BigDecimal> unitPriceCol;
    @FXML private TableColumn<MedicineDTO, String> statusCol;
    @FXML private TableColumn<MedicineDTO, Void> actionCol;

    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Pagination pagination;

    private MedicineService medicineService;
    private CategoryService categoryService;
    private BatchService batchService; // نحتاجه لجلب الأرصدة
    
    private ObservableList<MedicineDTO> masterDataList = FXCollections.observableArrayList();
    private FilteredList<MedicineDTO> filteredData;
    private List<Category> allCategories;
    private List<CheckBox> categoryCheckBoxes = new ArrayList<>();

    @FXML
    public void initialize() {
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        categoryService = new CategoryServiceImpl(new CategoryDAOImpl());
        batchService = new BatchServiceImpl(new BatchDAOImpl());

        setupTableColumns();
        setupPaginationControl();
        loadCategoriesIntoFilter(); 
        loadMedicines();
        
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void loadCategoriesIntoFilter() {
        allCategories = categoryService.getAllCategories();
        categoryFilterMenu.getItems().clear();
        categoryCheckBoxes.clear();

        for (Category cat : allCategories) {
            CheckBox cb = new CheckBox(cat.getName());
            cb.setUserData(cat.getCategory_id());
            cb.setOnAction(e -> {
                updateCategoryMenuText();
                applyFilters();
            });
            categoryCheckBoxes.add(cb);
            
            CustomMenuItem item = new CustomMenuItem(cb);
            item.setHideOnClick(false);
            categoryFilterMenu.getItems().add(item);
        }
        updateCategoryMenuText();
    }

    private void updateCategoryMenuText() {
        long selectedCount = categoryCheckBoxes.stream().filter(CheckBox::isSelected).count();
        if (selectedCount == 0 || selectedCount == categoryCheckBoxes.size()) {
            categoryFilterMenu.setText("كل التصنيفات");
        } else {
            categoryFilterMenu.setText(selectedCount + " تصنيفات محددة");
        }
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

        barcodeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicine().getBarcode()));
        brandNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicine().getBrand_name()));
        genericNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicine().getGeneric_name()));
        boxPriceCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMedicine().getCurrent_box_sell_price()));
        unitPriceCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getMedicine().getCurrent_unit_sell_price()));

        categoryCol.setCellValueFactory(cellData -> {
            int catId = cellData.getValue().getMedicine().getCategory_id();
            Optional<Category> cat = allCategories.stream().filter(c -> c.getCategory_id() == catId).findFirst();
            return new SimpleStringProperty(cat.isPresent() ? cat.get().getName() : "غير مصنف");
        });

        statusCol.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getMedicine().getIs_active() == 1 ? "✅ نشط" : "❌ موقوف");
        });

        // ==========================================
        // إعداد عمود الرصيد وتلوينه إذا كان دواء ناقصاً
        // ==========================================
        totalStockCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTotalStock()));
        totalStockCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    MedicineDTO dto = getTableRow().getItem();
                    // إذا كان الرصيد أقل من أو يساوي حد الطلب (min_stock_level) نلونه بالأحمر
                    if (item <= dto.getMedicine().getMin_stock_level()) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #fdeedb;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("تعديل ✏️");
            private final Button deleteBtn = new Button("حذف 🗑️");
            private final Button toggleBtn = new Button();
            private final HBox actions = new HBox(5, editBtn, deleteBtn, toggleBtn);
            {
                editBtn.getStyleClass().add("button-primary");
                deleteBtn.getStyleClass().add("button-danger");

                editBtn.setOnAction(e -> {
                    MedicineDTO dto = getTableView().getItems().get(getIndex());
                    ViewManager.getInstance().showModalWithData("/com/pharmacy/views/inventory/MedicineFormDialog.fxml", "تعديل دواء", dto.getMedicine());
                    loadMedicines(); // تحديث بعد الإغلاق
                });
                
                deleteBtn.setOnAction(e -> {
                    MedicineDTO dto = getTableView().getItems().get(getIndex());
                    if (AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف الدواء نهائياً؟")) {
                        try {
                            if (medicineService.deleteMedicine(dto.getMedicine().getMed_id())) {
                                loadMedicines();
                                AlertManager.showSuccess("نجاح", "تم حذف الدواء بنجاح.");
                            }
                        } catch (IllegalStateException ex) {
                            AlertManager.showError("حذف مرفوض", ex.getMessage());
                        } catch (Exception ex) {
                            AlertManager.showError("خطأ", "فشل الحذف. يرجى إيقاف تفعيله بدلاً من ذلك.");
                        }
                    }
                });

                toggleBtn.setOnAction(e -> handleToggleStatus(getTableView().getItems().get(getIndex()).getMedicine()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    MedicineDTO dto = getTableRow().getItem();
                    if (dto.getMedicine().getIs_active() == 1) {
                        toggleBtn.setText("إيقاف 🛑");
                        toggleBtn.getStyleClass().setAll("button", "button-danger");
                    } else {
                        toggleBtn.setText("تنشيط ♻️");
                        toggleBtn.getStyleClass().setAll("button", "button-success");
                    }
                    setGraphic(actions);
                }
            }
        });
    }

    private void loadMedicines() {
        // 1. جلب كل الأدوية
        List<Medicine> meds = medicineService.getAllMedicines();
        
        // 2. جلب كل التشغيلات الفعالة لتجميع الأرصدة بسرعة في الذاكرة
        List<Batch> activeBatches = batchService.getAllBatches().stream()
                .filter(b -> b.getIs_active() == 1)
                .collect(Collectors.toList());
                
        // تجميع الكميات حسب med_id (O(1) Mapping)
        Map<Integer, Integer> stockMap = activeBatches.stream()
                .collect(Collectors.groupingBy(Batch::getMed_id, Collectors.summingInt(Batch::getQuantity)));

        // 3. بناء قائمة الـ DTO
        List<MedicineDTO> dtoList = meds.stream()
                .map(m -> new MedicineDTO(m, stockMap.getOrDefault(m.getMed_id(), 0)))
                .collect(Collectors.toList());

        masterDataList.setAll(dtoList);
        applyFilters();
    }

    private void setupPaginationControl() {
        rowsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        rowsPerPageCombo.setValue(20);
        rowsPerPageCombo.valueProperty().addListener((obs, old, newVal) -> updatePagination());
        pagination.currentPageIndexProperty().addListener((obs, old, newVal) -> updateTableData(newVal.intValue()));
    }

    @FXML
    private void applyFilters() {
        filteredData = new FilteredList<>(masterDataList, dto -> {
            Medicine med = dto.getMedicine();
            
            // 1. فلتر الحالة (موقوف)
            boolean showInactive = showInactiveCheckBox.isSelected();
            if (!showInactive && med.getIs_active() == 0) return false;

            // 2. فلتر النواقص
            boolean showLowStock = showLowStockCheckBox.isSelected();
            if (showLowStock && dto.getTotalStock() > med.getMin_stock_level()) return false;

            // 3. فلتر البحث النصي
            String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            boolean matchesSearch = search.isEmpty() || 
                                    (med.getBrand_name() != null && med.getBrand_name().toLowerCase().contains(search)) ||
                                    (med.getGeneric_name() != null && med.getGeneric_name().toLowerCase().contains(search)) ||
                                    (med.getBarcode() != null && med.getBarcode().contains(search));
            if (!matchesSearch) return false;

            // 4. فلتر التصنيفات
            List<Integer> selectedIds = categoryCheckBoxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (Integer) cb.getUserData())
                    .collect(Collectors.toList());

            if (selectedIds.isEmpty()) return true;
            return selectedIds.contains(med.getCategory_id());
        });

        updatePagination();
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
        medicinesTable.setItems(FXCollections.observableArrayList(from < to ? filteredData.subList(from, to) : new ArrayList<>()));
    }

    private void handleToggleStatus(Medicine med) {
        String action = med.getIs_active() == 1 ? "إيقاف" : "تنشيط";
        if (AlertManager.showConfirmation("تأكيد", "هل تريد " + action + " الدواء: " + med.getBrand_name() + "؟")) {
            med.setIs_active(med.getIs_active() == 1 ? 0 : 1);
            if (medicineService.updateMedicine(med)) {
                loadMedicines();
                AlertManager.showSuccess("نجاح", "تمت العملية بنجاح.");
            }
        }
    }

    @FXML
    private void handleClearFilters(ActionEvent event) {
        searchField.clear();
        categoryCheckBoxes.forEach(cb -> cb.setSelected(false));
        showInactiveCheckBox.setSelected(false);
        showLowStockCheckBox.setSelected(false); // تفريغ فلتر النواقص
        updateCategoryMenuText();
        applyFilters();
    }

    @FXML
    private void handleApplyFilters(ActionEvent event) { applyFilters(); }

    @FXML
    private void handleAddNewMedicine(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/inventory/MedicineFormDialog.fxml", "إضافة دواء جديد", null);
        loadMedicines();
    }

    // ==========================================
    // كلاس DTO لدمج الدواء مع الرصيد الحالي
    // ==========================================
    public static class MedicineDTO {
        private final Medicine medicine;
        private final int totalStock;

        public MedicineDTO(Medicine medicine, int totalStock) {
            this.medicine = medicine;
            this.totalStock = totalStock;
        }

        public Medicine getMedicine() { return medicine; }
        public int getTotalStock() { return totalStock; }
    }
}