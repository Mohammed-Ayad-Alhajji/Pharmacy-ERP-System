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

public class CloseShiftController {

    @FXML private Label cashierNameLabel;
    @FXML private TextField actualCashField;
    @FXML private TextField notesField;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;

    private boolean shiftClosedSuccessfully = false;
    private ShiftService shiftService;

    @FXML
    public void initialize() {
        this.shiftService = new ShiftServiceImpl(new ShiftDAOImpl());

        if (SessionManager.getInstance().isLoggedIn()) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            cashierNameLabel.setText(currentUser.getFull_name());
        }

        actualCashField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                actualCashField.setText(oldValue);
            }
        });

        // التركيز التلقائي لراحة الكاشير
        Platform.runLater(() -> actualCashField.requestFocus());
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        String actualText = actualCashField.getText().trim();
        String notes = notesField.getText().trim(); // جاهزة للاستخدام المستقبلي

        if (actualText.isEmpty() || actualText.equals(".")) {
            showError("يرجى عد الدرج وإدخال الرصيد الفعلي أولاً بشكل صحيح.");
            return;
        }

        boolean confirmed = AlertManager.showConfirmation("تأكيد إغلاق الوردية", "هل أنت متأكد من إغلاق الوردية الحالية؟ لا يمكن التراجع عن هذه العملية أو إجراء مبيعات بعدها.");
        
        if (!confirmed) {
            return; 
        }

        try {
            BigDecimal actualCash = new BigDecimal(actualText);
            Shift currentShift = SessionManager.getInstance().getCurrentShift();

            if (currentShift == null) {
                AlertManager.showError("خطأ", "لا توجد وردية مفتوحة حالياً في الجلسة.");
                return;
            }

            shiftService.closeShift(currentShift.getShift_id(), actualCash);
            SessionManager.getInstance().clearShift();
            
            AlertManager.showSuccess("تم الإغلاق بنجاح", "تم تسجيل النقد الفعلي وإغلاق الوردية بنجاح.");
            
            shiftClosedSuccessfully = true;
            closeWindow();
            
        } catch (NumberFormatException e) {
            showError("صيغة الرقم المدخل غير صالحة.");
        } catch (ShiftException e) {
            AlertManager.showError("خطأ في الإغلاق", e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ", "حدث خطأ تقني غير متوقع أثناء إغلاق الوردية.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        shiftClosedSuccessfully = false;
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

    public boolean isShiftClosedSuccessfully() {
        return shiftClosedSuccessfully;
    }
}