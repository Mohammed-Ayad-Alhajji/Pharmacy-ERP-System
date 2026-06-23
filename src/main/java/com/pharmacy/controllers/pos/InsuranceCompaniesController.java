package com.pharmacy.controllers.pos;

import com.pharmacy.dao.impl.pos.InsuranceCompanyDAOImpl;
import com.pharmacy.models.pos.InsuranceCompany;
import com.pharmacy.services.impl.pos.InsuranceCompanyServiceImpl;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class InsuranceCompaniesController {

    @FXML private TextField searchField;
    @FXML private TableView<InsuranceCompany> companiesTable;
    @FXML private TableColumn<InsuranceCompany, String> idCol;
    @FXML private TableColumn<InsuranceCompany, String> nameCol;
    @FXML private TableColumn<InsuranceCompany, String> contactCol;
    @FXML private TableColumn<InsuranceCompany, String> addressCol;
    @FXML private TableColumn<InsuranceCompany, Void> actionCol;

    // أدوات الترقيم
    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Pagination pagination;

    private InsuranceCompanyService companyService;
    
    private ObservableList<InsuranceCompany> masterDataList = FXCollections.observableArrayList();
    private FilteredList<InsuranceCompany> filteredData;

    @FXML
    public void initialize() {
        companyService = new InsuranceCompanyServiceImpl(new InsuranceCompanyDAOImpl());

        setupTableColumns();
        setupPaginationControl();
        
        loadCompanies();

        // تفعيل البحث اللحظي وربطه مع الفلتر والترقيم
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupPaginationControl() {
        rowsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        rowsPerPageCombo.setValue(20);
        rowsPerPageCombo.valueProperty().addListener((obs, old, newVal) -> updatePagination());
        pagination.currentPageIndexProperty().addListener((obs, old, newVal) -> updateTableData(newVal.intValue()));
    }

    private void applyFilters() {
        if (filteredData == null) return;
        
        filteredData.setPredicate(company -> {
            String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            if (query.isEmpty()) return true;
            return company.getName().toLowerCase().contains(query);
        });
        
        updatePagination();
    }

    private void updatePagination() {
        if (filteredData == null) return;
        int rowsPerPage = rowsPerPageCombo.getValue();
        int pageCount = (int) Math.ceil((double) filteredData.size() / rowsPerPage);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        updateTableData(0);
    }

    private void updateTableData(int pageIndex) {
        if (filteredData == null) return;
        int rowsPerPage = rowsPerPageCombo.getValue();
        int from = pageIndex * rowsPerPage;
        int to = Math.min(from + rowsPerPage, filteredData.size());
        
        if (from < to) {
            companiesTable.setItems(FXCollections.observableArrayList(filteredData.subList(from, to)));
        } else {
            companiesTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getInsurance_id())));
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        contactCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContact_info() != null ? c.getValue().getContact_info() : "-"));
        addressCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress() != null ? c.getValue().getAddress() : "-"));

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("تعديل ✏️");
            private final Button deleteBtn = new Button("حذف 🗑️");
            private final HBox actions = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("button-primary");
                editBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold;"); 
                
                deleteBtn.getStyleClass().add("button-danger");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;"); 

                editBtn.setOnAction(e -> {
                    InsuranceCompany selected = getTableView().getItems().get(getIndex());
                    ViewManager.getInstance().showModalWithData("/com/pharmacy/views/pos/InsuranceCompanyFormDialog.fxml", "تعديل بيانات الشركة", selected);
                    loadCompanies(); 
                });

                deleteBtn.setOnAction(e -> {
                    InsuranceCompany selected = getTableView().getItems().get(getIndex());
                    
                    if (AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف شركة '" + selected.getName() + "'؟\nملاحظة: النظام يمنع حذف أي شركة لها مطالبات مالية.")) {
                        if (companyService.deleteCompany(selected.getInsurance_id())) {
                            AlertManager.showSuccess("نجاح", "تم حذف شركة التأمين بنجاح.");
                            loadCompanies();
                        } else {
                            AlertManager.showError("حذف مرفوض", "لا يمكن حذف هذه الشركة لوجود فواتير تأمين أو مطالبات مالية مرتبطة بها.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    private void loadCompanies() {
        masterDataList.setAll(companyService.getAllCompanies());
        filteredData = new FilteredList<>(masterDataList, p -> true); // عرض الكل في البداية
        applyFilters();
    }

    @FXML
    private void handleAddNewCompany(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/pos/InsuranceCompanyFormDialog.fxml", "إضافة شركة تأمين", null);
        loadCompanies(); 
        searchField.clear();
    }
}