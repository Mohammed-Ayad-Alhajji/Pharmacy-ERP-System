package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.CategoryDAOImpl;
import com.pharmacy.models.inventory.Category;
import com.pharmacy.services.impl.inventory.CategoryServiceImpl;
import com.pharmacy.services.interfaces.inventory.CategoryService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class MedicineCategoryController {

    @FXML private TextField searchField;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> idCol;
    @FXML private TableColumn<Category, String> nameCol;
    @FXML private TableColumn<Category, String> descCol;
    @FXML private TableColumn<Category, Void> actionCol;

    // عناصر نظام الصفحات (Pagination)
    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Pagination pagination;

    private CategoryService categoryService;
    private ObservableList<Category> masterData = FXCollections.observableArrayList();
    private FilteredList<Category> filteredData; // نرفعها لتكون عامة لنتمكن من تقطيعها

    @FXML
    public void initialize() {
        categoryService = new CategoryServiceImpl(new CategoryDAOImpl());
        
        setupTable();
        setupPaginationControl(); // تهيئة أزرار الصفحات
        loadData();
    }

    private void setupTable() {
        // الترقيم التسلسلي المستمر بدلاً من ID قاعدة البيانات
        idCol.setCellValueFactory(param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(null));
        idCol.setCellFactory(col -> new TableCell<Category, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                } else {
                    // حماية إضافية ضد الـ Null عند بدء التشغيل
                    int currentPage = pagination != null ? pagination.getCurrentPageIndex() : 0;
                    int rowsPerPage = (rowsPerPageCombo != null && rowsPerPageCombo.getValue() != null) ? rowsPerPageCombo.getValue() : 20;
                    int offset = currentPage * rowsPerPage;
                    setText(String.valueOf(offset + getIndex() + 1));
                }
            }
        });
        
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("تعديل ✏️");
            private final Button deleteBtn = new Button("حذف 🗑️");
            private final HBox actions = new HBox(10, editBtn, deleteBtn);
            {
                // الألوان
                editBtn.getStyleClass().add("button-primary");
                deleteBtn.getStyleClass().add("button-danger");

                // حدث زر التعديل (الملف الموحد الصحيح)
                editBtn.setOnAction(e -> {
                    Category cat = getTableView().getItems().get(getIndex());
                    ViewManager.getInstance().showModalWithData("/com/pharmacy/views/inventory/CategoryFormDialog.fxml", "تعديل الصنف", cat);
                    loadData();
                });
                
                // حدث زر الحذف
                deleteBtn.setOnAction(e -> {
                    Category cat = getTableView().getItems().get(getIndex());
                    handleDelete(cat);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    // --- نظام تقسيم الصفحات ---
    private void setupPaginationControl() {
        // تعبئة القائمة المنسدلة بخيارات عدد السطور
        rowsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        rowsPerPageCombo.setValue(20); // القيمة الافتراضية
        
        // عند تغيير عدد السطور، أعد حساب الصفحات
        rowsPerPageCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePagination());
        
        // عند الضغط على زر صفحة جديدة (1, 2, 3...)
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updateTableData(newIndex.intValue());
        });
    }

    private void updatePagination() {
        if (filteredData == null) return;
        
        int totalItems = filteredData.size();
        int rowsPerPage = rowsPerPageCombo.getValue();
        int pageCount = (int) Math.ceil((double) totalItems / rowsPerPage);
        
        if (pageCount == 0) pageCount = 1; // يجب أن يكون هناك صفحة واحدة على الأقل
        
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0); // العودة للصفحة الأولى دائماً عند الفلترة
        updateTableData(0);
    }

    private void updateTableData(int pageIndex) {
        int rowsPerPage = rowsPerPageCombo.getValue();
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, filteredData.size());
        
        // استقطاع جزء من البيانات لعرضه في الجدول
        if (fromIndex < toIndex) {
            categoryTable.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        } else {
            categoryTable.setItems(FXCollections.observableArrayList());
        }
    }
    // ----------------------------

    private void loadData() {
        masterData.setAll(categoryService.getAllCategories());
        filteredData = new FilteredList<>(masterData, p -> true);
        
        // ربط حقل البحث
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(cat -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return cat.getName().toLowerCase().contains(newVal.toLowerCase());
            });
            // بعد البحث، يجب تحديث عدد الصفحات
            updatePagination();
        });
        
        // تهيئة العرض لأول مرة
        updatePagination();
    }

    @FXML
    private void handleAddNewCategory(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/inventory/CategoryFormDialog.fxml", "إضافة صنف", null);
        loadData();
    }

    private void handleDelete(Category cat) {
        if (AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف هذا الصنف؟")) {
            try {
                if (categoryService.deleteCategory(cat.getCategory_id())) {
                    loadData();
                    AlertManager.showSuccess("نجاح", "تم حذف الصنف بنجاح.");
                }
            } catch (Exception e) {
                AlertManager.showError("خطأ", "لا يمكن حذف الصنف لارتباطه بأدوية مسجلة.");
            }
        }
    }
}