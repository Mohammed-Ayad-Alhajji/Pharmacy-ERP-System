package com.pharmacy.controllers.pos;

import com.pharmacy.dao.impl.pos.InsuranceCompanyDAOImpl;
import com.pharmacy.models.pos.InsuranceCompany;
import com.pharmacy.services.impl.pos.InsuranceCompanyServiceImpl;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
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

public class InsuranceCompanyFormController implements DataTransferable {

    @FXML private Label formTitleLabel;
    @FXML private TextField nameField;
    @FXML private TextField contactField;
    @FXML private TextField addressField;
    @FXML private Button saveBtn;

    private InsuranceCompanyService companyService;
    private InsuranceCompany currentCompany;

    @FXML
    public void initialize() {
        companyService = new InsuranceCompanyServiceImpl(new InsuranceCompanyDAOImpl());
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof InsuranceCompany) {
            this.currentCompany = (InsuranceCompany) data;
            fillFormForEdit();
        } else {
            this.currentCompany = null;
            formTitleLabel.setText("إضافة شركة تأمين 🏥");
        }
    }

    private void fillFormForEdit() {
        formTitleLabel.setText("تعديل بيانات الشركة ✏️");
        saveBtn.setText("تحديث البيانات 💾");

        nameField.setText(currentCompany.getName());
        contactField.setText(currentCompany.getContact_info() != null ? currentCompany.getContact_info() : "");
        addressField.setText(currentCompany.getAddress() != null ? currentCompany.getAddress() : "");
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            AlertManager.showWarning("بيانات ناقصة", "اسم الشركة إلزامي ولا يمكن تركه فارغاً.");
            return;
        }

        try {
            boolean isNew = (currentCompany == null);
            InsuranceCompany company = isNew ? new InsuranceCompany() : currentCompany;

            company.setName(name);
            
            String contact = contactField.getText().trim();
            company.setContact_info(contact.isEmpty() ? null : contact);
            
            String address = addressField.getText().trim();
            company.setAddress(address.isEmpty() ? null : address);

            if (isNew) {
                Optional<InsuranceCompany> saved = companyService.createCompany(company);
                if (saved.isPresent()) {
                    AlertManager.showSuccess("نجاح", "تم تسجيل شركة التأمين بنجاح.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "حدث خطأ أثناء حفظ بيانات الشركة.");
                }
            } else {
                if (companyService.updateCompany(company)) {
                    AlertManager.showSuccess("نجاح", "تم تحديث بيانات الشركة بنجاح.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "فشل التحديث. يرجى المحاولة مرة أخرى.");
                }
            }
        } catch (IllegalArgumentException ex) {
            // سيلتقط رسالة "الشركة مسجلة مسبقاً" التي يرميها الـ Service
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