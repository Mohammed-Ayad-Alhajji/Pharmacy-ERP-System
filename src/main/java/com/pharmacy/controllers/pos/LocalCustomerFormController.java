package com.pharmacy.controllers.pos;

import com.pharmacy.dao.impl.pos.LocalCustomerDAOImpl;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.services.impl.pos.LocalCustomerServiceImpl;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.DataTransferable;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class LocalCustomerFormController implements DataTransferable {

    @FXML private Label formTitleLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private Button saveBtn;

    private LocalCustomerService customerService;
    private LocalCustomer currentCustomer;

    @FXML
    public void initialize() {
        customerService = new LocalCustomerServiceImpl(new LocalCustomerDAOImpl());
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof LocalCustomer) {
            this.currentCustomer = (LocalCustomer) data;
            fillFormForEdit();
        } else {
            this.currentCustomer = null;
            formTitleLabel.setText("إضافة عميل جديد 👤");
        }
    }

    private void fillFormForEdit() {
        formTitleLabel.setText("تعديل بيانات العميل ✏️");
        saveBtn.setText("تحديث البيانات 💾");

        nameField.setText(currentCustomer.getName());
        phoneField.setText(currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "");
        addressField.setText(currentCustomer.getAddress() != null ? currentCustomer.getAddress() : "");
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertManager.showWarning("بيانات ناقصة", "اسم العميل إلزامي ولا يمكن تركه فارغاً.");
            return;
        }

        try {
            boolean isNew = (currentCustomer == null);
            LocalCustomer customer = isNew ? new LocalCustomer() : currentCustomer;

            customer.setName(name);
            
            String phone = phoneField.getText().trim();
            customer.setPhone(phone.isEmpty() ? null : phone);
            
            String address = addressField.getText().trim();
            customer.setAddress(address.isEmpty() ? null : address);

            if (isNew) {
                Optional<LocalCustomer> saved = customerService.createCustomer(customer);
                if (saved.isPresent()) {
                    AlertManager.showSuccess("نجاح", "تم تسجيل العميل بنجاح.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "حدث خطأ أثناء حفظ بيانات العميل.");
                }
            } else {
                if (customerService.updateCustomer(customer)) {
                    AlertManager.showSuccess("نجاح", "تم تحديث بيانات العميل بنجاح.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "فشل التحديث. يرجى المحاولة مرة أخرى.");
                }
            }
        } catch (IllegalArgumentException ex) {
            AlertManager.showError("خطأ في البيانات", ex.getMessage());
        } catch (Exception ex) {
            AlertManager.showError("خطأ", "حدث خطأ غير متوقع: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow(event);
    }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}