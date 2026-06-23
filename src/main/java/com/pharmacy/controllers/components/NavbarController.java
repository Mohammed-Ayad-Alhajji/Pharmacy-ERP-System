// المسار: src/main/java/com/pharmacy/controllers/components/NavbarController.java

package com.pharmacy.controllers.components;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.security.User;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.utils.gui.ViewManager;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NavbarController {

    @FXML private Button shiftStatusButton;
    @FXML private Button quickSaleButton; 
    @FXML private MenuButton receiptVoucherMenu;
    @FXML private Button supplierPaymentButton; 
    @FXML private Label navUserNameLabel;
    @FXML private Label navUserRoleLabel;

    // عناصر التنبيهات
    @FXML private Button expiryAlertsButton;
    @FXML private Label expiryBadge;
    @FXML private Button lowStockAlertsButton;
    @FXML private Label lowStockBadge;

    private MedicineService medicineService;
    private BatchService batchService;

    @FXML
    public void initialize() {
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        batchService = new BatchServiceImpl(new BatchDAOImpl());

        if (SessionManager.getInstance().isLoggedIn()) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            navUserNameLabel.setText(currentUser.getFull_name());
            
            String roleName = (currentUser.getRole_id() == 1) ? "مدير النظام" : "موظف مبيعات";
            navUserRoleLabel.setText(roleName);
        }
        updateShiftButtonState();
        
        loadAlertsCount();
    }

    private void loadAlertsCount() {
        Task<Void> alertsTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // ==========================================
                // 1. حساب النواقص الدقيق (نفس منطق شاشة الأدوية)
                // ==========================================
                // أ. جلب الأدوية النشطة فقط
                List<Medicine> activeMedicines = medicineService.getAllMedicines().stream()
                        .filter(m -> m.getIs_active() == 1)
                        .collect(Collectors.toList());

                // ب. جلب التشغيلات الفعالة لتجميع الأرصدة
                List<Batch> activeBatches = batchService.getAllBatches().stream()
                        .filter(b -> b.getIs_active() == 1)
                        .collect(Collectors.toList());

                // ج. تجميع الكميات لكل دواء
                Map<Integer, Integer> stockMap = activeBatches.stream()
                        .collect(Collectors.groupingBy(Batch::getMed_id, Collectors.summingInt(Batch::getQuantity)));

                // د. عد الأدوية التي رصيدها الفعلي أقل من أو يساوي الحد الأدنى
                int lowStockCount = (int) activeMedicines.stream()
                        .filter(m -> stockMap.getOrDefault(m.getMed_id(), 0) <= m.getMin_stock_level())
                        .count();

                // ==========================================
                // 2. حساب عدد التشغيلات المقاربة على الانتهاء
                // ==========================================
                int expiryCount = batchService.getNearExpiryBatches(90).size();

                // تحديث الواجهة الرسومية
                Platform.runLater(() -> {
                    if (lowStockCount > 0) {
                        lowStockBadge.setText(String.valueOf(lowStockCount));
                        lowStockBadge.setVisible(true);
                    } else {
                        lowStockBadge.setVisible(false);
                    }

                    if (expiryCount > 0) {
                        expiryBadge.setText(String.valueOf(expiryCount));
                        expiryBadge.setVisible(true);
                    } else {
                        expiryBadge.setVisible(false);
                    }
                });
                return null;
            }
        };
        new Thread(alertsTask).start();
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        ViewManager.getInstance().toggleSidebar();
    }

    @FXML
    private void handleShiftAction(ActionEvent event) {
        try {
            if (SessionManager.getInstance().getCurrentShift() == null) {
                ViewManager.getInstance().showModal("/com/pharmacy/views/finance/shifts/OpenShiftDialog.fxml", "بدء وردية جديدة");
            } else {
                ViewManager.getInstance().showModal("/com/pharmacy/views/finance/shifts/CloseShiftDialog.fxml", "إغلاق الوردية");
            }
            updateShiftButtonState();
        } catch (Exception e) {
            System.err.println("❌ خطأ أثناء محاولة فتح نافذة الوردية: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    @FXML
    private void handleQuickSale(ActionEvent event) {
        ViewManager.getInstance().switchScene("/com/pharmacy/views/pos/PosMainView.fxml");
    }

    @FXML
    private void handleCustomerPayment(ActionEvent event) {
        ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/CustomerPaymentsView.fxml");
    }

    @FXML
    private void handleInsurancePayment(ActionEvent event) {
        ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/InsurancePaymentsView.fxml");
    }

    @FXML
    private void handleSupplierPayment(ActionEvent event) {
        ViewManager.getInstance().switchScene("/com/pharmacy/views/purchases/SupplierPaymentsView.fxml");
    }

    @FXML
    private void handleLowStockAlerts(ActionEvent event) {
        ViewManager.getInstance().switchScene("/com/pharmacy/views/inventory/MedicineMainView.fxml");
    }

    @FXML
    private void handleExpiryAlerts(ActionEvent event) {
        ViewManager.getInstance().switchScene("/com/pharmacy/views/inventory/BatchMainView.fxml");
    }

    private void updateShiftButtonState() {
        if (SessionManager.getInstance().getCurrentShift() != null) {
            shiftStatusButton.setText("الوردية مفتوحة 🟢");
            shiftStatusButton.getStyleClass().remove("shift-closed-btn");
            if (!shiftStatusButton.getStyleClass().contains("shift-open-btn")) {
                shiftStatusButton.getStyleClass().add("shift-open-btn");
            }
        } else {
            shiftStatusButton.setText("بدء وردية 🔴");
            shiftStatusButton.getStyleClass().remove("shift-open-btn");
            if (!shiftStatusButton.getStyleClass().contains("shift-closed-btn")) {
                shiftStatusButton.getStyleClass().add("shift-closed-btn");
            }
        }
    }
}