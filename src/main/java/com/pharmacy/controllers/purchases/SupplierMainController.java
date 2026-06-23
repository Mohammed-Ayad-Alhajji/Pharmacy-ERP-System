package com.pharmacy.controllers.purchases;

// تأكد من تعديل مسارات الـ Imports لتطابق بنية مشروعك
import com.pharmacy.dao.impl.purchasing.SupplierDAOImpl;

import com.pharmacy.models.purchasing.Supplier;
import com.pharmacy.services.impl.purchasing.SupplierServiceImpl;
import com.pharmacy.services.interfaces.purchasing.SupplierService;
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

public class SupplierMainController {

    @FXML private TextField searchField;
    
    @FXML private TableView<Supplier> suppliersTable;
    @FXML private TableColumn<Supplier, Integer> indexCol;
    @FXML private TableColumn<Supplier, String> nameCol;
    @FXML private TableColumn<Supplier, String> contactCol;
    @FXML private TableColumn<Supplier, String> phoneCol;
    @FXML private TableColumn<Supplier, String> addressCol;
    @FXML private TableColumn<Supplier, Void> actionCol;

    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Pagination pagination;

    private SupplierService supplierService;
    private ObservableList<Supplier> masterDataList = FXCollections.observableArrayList();
    private FilteredList<Supplier> filteredData;

    @FXML
    public void initialize() {
        // تهيئة الخدمة
        supplierService = new SupplierServiceImpl(new SupplierDAOImpl());

        setupTableColumns();
        setupPaginationControl();
        setupFilters();
        
        loadSuppliers();
    }

    private void setupTableColumns() {
        // الترقيم التسلسلي المستمر
        indexCol.setCellValueFactory(param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(null));
        indexCol.setCellFactory(col -> new TableCell<Supplier, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                } else {
                    int currentPage = pagination != null ? pagination.getCurrentPageIndex() : 0;
                    int rowsPerPage = (rowsPerPageCombo != null && rowsPerPageCombo.getValue() != null) ? rowsPerPageCombo.getValue() : 20;
                    int offset = currentPage * rowsPerPage;
                    setText(String.valueOf(offset + getIndex() + 1));
                }
            }
        });

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contact_person"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("تعديل ✏️");
            private final Button deleteBtn = new Button("حذف 🗑️");
            private final HBox actions = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("button-primary");
                deleteBtn.getStyleClass().add("button-danger");

                // استدعاء واجهة التعديل الذكية الموحدة (سنقوم ببنائها تالياً)
                editBtn.setOnAction(e -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());
                    ViewManager.getInstance().showModalWithData("/com/pharmacy/views/purchases/SupplierFormDialog.fxml", "تعديل بيانات المورد", supplier);
                    loadSuppliers();
                });

                deleteBtn.setOnAction(e -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());
                    handleDeleteSupplier(supplier);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadSuppliers() {
        masterDataList.setAll(supplierService.getAllSuppliers());
        applyFilters();
    }

    private void applyFilters() {
        filteredData = new FilteredList<>(masterDataList, supplier -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            if (searchText.isEmpty()) {
                return true;
            }

            // البحث مرن يغطي الاسم، المندوب، والهاتف
            return (supplier.getName() != null && supplier.getName().toLowerCase().contains(searchText)) ||
                   (supplier.getContact_person() != null && supplier.getContact_person().toLowerCase().contains(searchText)) ||
                   (supplier.getPhone() != null && supplier.getPhone().contains(searchText));
        });

        updatePagination();
    }

    private void setupPaginationControl() {
        rowsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        rowsPerPageCombo.setValue(20);
        rowsPerPageCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePagination());
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> updateTableData(newIndex.intValue()));
    }

    private void updatePagination() {
        if (filteredData == null) return;
        int totalItems = filteredData.size();
        int rowsPerPage = rowsPerPageCombo.getValue() != null ? rowsPerPageCombo.getValue() : 20;
        int pageCount = (int) Math.ceil((double) totalItems / rowsPerPage);
        
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        updateTableData(0);
    }

    private void updateTableData(int pageIndex) {
        if (filteredData == null) return;
        int rowsPerPage = rowsPerPageCombo.getValue() != null ? rowsPerPageCombo.getValue() : 20;
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, filteredData.size());
        
        if (fromIndex < toIndex) {
            suppliersTable.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        } else {
            suppliersTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void handleDeleteSupplier(Supplier supplier) {
        if (AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف المورد: " + supplier.getName() + "؟\n(ملاحظة: لا يمكن حذف مورد لديه فواتير مشتريات سابقة)")) {
            try {
                // التعديل هنا: استخدام دالة deleteSupplier الصحيحة
                if (supplierService.deleteSupplier(supplier.getSupplier_id())) {
                    AlertManager.showSuccess("نجاح", "تم حذف المورد بنجاح.");
                    loadSuppliers(); // تحديث الجدول بعد الحذف
                } else {
                    AlertManager.showError("خطأ", "لم يتم الحذف. قد يكون المورد مرتبطاً بفواتير سابقة في النظام.");
                }
            } catch (Exception e) {
                AlertManager.showError("عملية محظورة", "لا يمكن حذف هذا المورد لارتباطه بسجلات مالية في النظام.");
            }
        }
    }

    @FXML
    private void handleClearFilters(ActionEvent event) {
        searchField.clear();
    }

    @FXML
    private void handleAddNewSupplier(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/purchases/SupplierFormDialog.fxml", "إضافة مورد جديد", null);
        loadSuppliers();
    }
}