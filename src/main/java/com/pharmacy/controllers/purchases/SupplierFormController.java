package com.pharmacy.controllers.purchasing;

import com.pharmacy.models.purchasing.Supplier;
import com.pharmacy.services.interfaces.purchasing.SupplierService;
import com.pharmacy.services.impl.purchasing.SupplierServiceImpl;
import com.pharmacy.dao.impl.purchasing.SupplierDAOImpl;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.DataTransferable;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class SupplierFormController implements DataTransferable {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private TextField contactField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressArea;
    @FXML private Button saveBtn;

    private SupplierService supplierService;
    private Supplier currentSupplier;

    @FXML
    public void initialize() {
        // تهيئة الخدمة
        supplierService = new SupplierServiceImpl(new SupplierDAOImpl());
    }

    @Override
    public void receiveData(Object data) {
        if (data != null && data instanceof Supplier) {
            // وضع التعديل (Edit Mode)
            this.currentSupplier = (Supplier) data;
            populateFields();
            titleLabel.setText("تعديل بيانات المورد");
            saveBtn.setText("تحديث البيانات");
        } else {
            // وضع الإضافة (Add Mode)
            this.currentSupplier = null;
            titleLabel.setText("إضافة مورد جديد");
            saveBtn.setText("حفظ المورد");
        }
    }

    private void populateFields() {
        nameField.setText(currentSupplier.getName());
        contactField.setText(currentSupplier.getContact_person() != null ? currentSupplier.getContact_person() : "");
        phoneField.setText(currentSupplier.getPhone() != null ? currentSupplier.getPhone() : "");
        addressArea.setText(currentSupplier.getAddress() != null ? currentSupplier.getAddress() : "");
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressArea.getText().trim();

        // تحقق أساسي من البيانات (Validation)
        if (name.isEmpty()) {
            AlertManager.showError("بيانات ناقصة", "حقل 'اسم المورد / الشركة' مطلوب ولا يمكن تركه فارغاً.");
            return;
        }

        if (currentSupplier == null) {
            // تنفيذ عملية الإضافة
            Supplier newSupplier = new Supplier(name, contact, phone, address);
            Optional<Supplier> saved = supplierService.createSupplier(newSupplier);
            
            if (saved.isPresent()) {
                AlertManager.showSuccess("نجاح", "تمت إضافة المورد الجديد بنجاح.");
                closeWindow(event);
            } else {
                AlertManager.showError("خطأ", "فشل إضافة المورد. قد يكون الاسم مسجلاً مسبقاً في النظام.");
            }
        } else {
            // تنفيذ عملية التعديل
            currentSupplier.setName(name);
            currentSupplier.setContact_person(contact);
            currentSupplier.setPhone(phone);
            currentSupplier.setAddress(address);

            if (supplierService.updateSupplier(currentSupplier)) {
                AlertManager.showSuccess("نجاح", "تم تحديث بيانات المورد بنجاح.");
                closeWindow(event);
            } else {
                AlertManager.showError("خطأ", "فشل تحديث بيانات المورد.");
            }
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