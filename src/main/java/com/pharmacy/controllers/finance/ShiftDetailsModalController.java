package com.pharmacy.controllers.finance.shifts;

import com.pharmacy.dao.impl.security.ShiftDAOImpl;
import com.pharmacy.models.finance.ShiftFinancialSummary;
import com.pharmacy.services.impl.security.ShiftServiceImpl;
import com.pharmacy.services.interfaces.security.ShiftService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ShiftDetailsModalController {

    @FXML private Label lblShiftId;
    
    // الموجب
    @FXML private Label lblOpening, lblCashSales, lblCustomerReceipts, lblInsuranceReceipts, lblSupplierRefunds;
    
    // السالب
    @FXML private Label lblPatientRefunds, lblSupplierPayments, lblExpenses;
    
    // الخلاصة
    @FXML private Label lblExpected, lblActual, lblDifference;

    private ShiftService shiftService;

    @FXML
    public void initialize() {
        this.shiftService = new ShiftServiceImpl(new ShiftDAOImpl());
    }

    // هذه الدالة سيتم استدعاؤها من الكنترولر الرئيسي لتمرير رقم الوردية
    public void initData(int shiftId) {
        lblShiftId.setText(String.valueOf(shiftId));
        
        // جلب البيانات من قاعدة البيانات باستخدام الدالة العبقرية التي صنعناها
        ShiftFinancialSummary summary = shiftService.getShiftFinancialDetails(shiftId);
        
        Platform.runLater(() -> {
            lblOpening.setText(formatMoney(summary.openingBalance));
            lblCashSales.setText(formatMoney(summary.cashSales));
            lblCustomerReceipts.setText(formatMoney(summary.customerReceipts));
            lblInsuranceReceipts.setText(formatMoney(summary.insuranceReceipts));
            lblSupplierRefunds.setText(formatMoney(summary.supplierRefunds));
            
            lblPatientRefunds.setText(formatMoney(summary.patientRefunds));
            lblSupplierPayments.setText(formatMoney(summary.supplierPayments));
            lblExpenses.setText(formatMoney(summary.expenses));
            
            lblExpected.setText(formatMoney(summary.expectedBalance));
            
            // إذا كانت الوردية ما تزال مفتوحة، سنعرض الفعلي كـ (قيد العمل)
            if (summary.actualBalance.compareTo(BigDecimal.ZERO) == 0 && summary.difference.compareTo(BigDecimal.ZERO) == 0 && summary.expectedBalance.compareTo(BigDecimal.ZERO) > 0) {
                 // هذا يعني غالباً أنها لم تغلق بعد
                 lblActual.setText("---");
                 lblDifference.setText("الوردية قيد العمل ولم تغلق بعد");
                 lblDifference.setStyle("-fx-text-fill: #95a5a6; -fx-font-weight: bold; -fx-font-size: 16px;");
            } else {
                lblActual.setText(formatMoney(summary.actualBalance));
                
                if (summary.difference.compareTo(BigDecimal.ZERO) == 0) {
                    lblDifference.setText("متطابق تماماً ✓");
                    lblDifference.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 18px;");
                } else if (summary.difference.compareTo(BigDecimal.ZERO) < 0) {
                    lblDifference.setText("يوجد عجز في الصندوق بقيمة: " + formatMoney(summary.difference.abs()));
                    lblDifference.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold; -fx-font-size: 18px;");
                } else {
                    lblDifference.setText("يوجد زيادة غير مبررة بقيمة: " + formatMoney(summary.difference));
                    lblDifference.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 18px;");
                }
            }
        });
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ل.س";
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return String.format("%,d ل.س", rounded.toBigInteger());
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) lblShiftId.getScene().getWindow();
        stage.close();
    }
}