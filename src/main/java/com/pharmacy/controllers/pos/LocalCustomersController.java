package com.pharmacy.controllers.pos;

import com.pharmacy.dao.impl.pos.LocalCustomerDAOImpl;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.services.impl.pos.LocalCustomerServiceImpl;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

public class LocalCustomersController {

    @FXML private TextField searchField;
    @FXML private TableView<LocalCustomer> customersTable;
    @FXML private TableColumn<LocalCustomer, String> idCol;
    @FXML private TableColumn<LocalCustomer, String> nameCol;
    @FXML private TableColumn<LocalCustomer, String> phoneCol;
    @FXML private TableColumn<LocalCustomer, String> addressCol;
    @FXML private TableColumn<LocalCustomer, Void> actionCol;

    private LocalCustomerService customerService;

    @FXML
    public void initialize() {
        customerService = new LocalCustomerServiceImpl(new LocalCustomerDAOImpl());

        setupTableColumns();
        loadCustomers();

        // تفعيل البحث اللحظي بمجرد الكتابة
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            List<LocalCustomer> results = customerService.searchCustomers(newVal);
            customersTable.setItems(FXCollections.observableArrayList(results));
        });
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCustomer_id())));
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        phoneCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone() != null ? c.getValue().getPhone() : "-"));
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
                    LocalCustomer selected = getTableView().getItems().get(getIndex());
                    ViewManager.getInstance().showModalWithData("/com/pharmacy/views/pos/LocalCustomerFormDialog.fxml", "تعديل بيانات العميل", selected);
                    loadCustomers(); 
                });

                deleteBtn.setOnAction(e -> {
                    LocalCustomer selected = getTableView().getItems().get(getIndex());
                    
                    if (AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف العميل '" + selected.getName() + "' نهائياً؟\nملاحظة: لا يمكنك حذف عميل له فواتير أو حركات مالية مسجلة.")) {
                        if (customerService.deleteCustomer(selected.getCustomer_id())) {
                            AlertManager.showSuccess("نجاح", "تم حذف العميل بنجاح.");
                            loadCustomers();
                        } else {
                            AlertManager.showError("حذف مرفوض", "لا يمكن حذف هذا العميل لوجود فواتير، ديون، أو سندات دفع مرتبطة به في النظام.");
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

    private void loadCustomers() {
        List<LocalCustomer> allCustomers = customerService.getAllCustomers();
        customersTable.setItems(FXCollections.observableArrayList(allCustomers));
    }

    @FXML
    private void handleAddNewCustomer(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/pos/LocalCustomerFormDialog.fxml", "إضافة عميل جديد", null);
        loadCustomers(); 
        searchField.clear();
    }
}