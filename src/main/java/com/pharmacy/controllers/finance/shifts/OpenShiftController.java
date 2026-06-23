package com.pharmacy.controllers.finance.shifts;

import com.pharmacy.dao.impl.security.ShiftDAOImpl;
import com.pharmacy.models.security.Shift;
import com.pharmacy.models.security.User;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.impl.security.ShiftServiceImpl;
import com.pharmacy.services.interfaces.security.ShiftService;
import com.pharmacy.utils.exceptions.ShiftException;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class OpenShiftController {

    @FXML private Label cashierNameLabel;
    @FXML private TextField openingBalanceField;
    @FXML private TextField notesField;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button confirmButton;

    private boolean shiftOpenedSuccessfully = false;
    private ShiftService shiftService;

    @FXML
    public void initialize() {
        this.shiftService = new ShiftServiceImpl(new ShiftDAOImpl());

        if (SessionManager.getInstance().isLoggedIn()) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            cashierNameLabel.setText(currentUser.getFull_name());
        }

        openingBalanceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                openingBalanceField.setText(oldValue);
            }
        });

        // اللمسة السحرية 1: وضع المؤشر تلقائياً في حقل المبلغ عند فتح النافذة
        Platform.runLater(() -> openingBalanceField.requestFocus());
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        String amountText = openingBalanceField.getText().trim();
        // اللمسة السحرية 2: قراءة الملاحظات لتكون جاهزة مستقبلاً
        String notes = notesField.getText().trim(); 
        
        if (amountText.isEmpty() || amountText.equals(".")) {
            showError("يرجى إدخال الرصيد الافتتاحي للدرج بشكل صحيح.");
            return;
        }

        try {
            BigDecimal openingBalance = new BigDecimal(amountText);
            int currentUserId = SessionManager.getInstance().getCurrentUser().getUser_id();

            // ملاحظة: إذا قمت بتحديث ShiftService لاحقاً لدعم الملاحظات، يمكنك تمرير متغير 'notes' هنا
            Shift newShift = shiftService.openShift(currentUserId, openingBalance);

            SessionManager.getInstance().setCurrentShift(newShift);
            
            AlertManager.showSuccess("تم الفتح بنجاح", "تم فتح الوردية بنجاح. يمكنك الآن بدء المبيعات واستلام الأموال.");
            
            shiftOpenedSuccessfully = true;
            closeWindow();

        } catch (NumberFormatException e) {
            showError("الرقم المدخل غير صالح.");
        } catch (ShiftException e) {
            AlertManager.showWarning("تحذير", e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ", "حدث خطأ غير متوقع أثناء فتح الوردية.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        shiftOpenedSuccessfully = false;
        closeWindow();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public boolean isShiftOpenedSuccessfully() {
        return shiftOpenedSuccessfully;
    }
}