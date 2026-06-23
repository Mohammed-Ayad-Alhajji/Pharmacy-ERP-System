package com.pharmacy.controllers.components;

import com.pharmacy.models.security.User;
import com.pharmacy.security.SessionManager;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SidebarController {

    @FXML private VBox sidebar;
    @FXML private Button logoutButton;

    @FXML private Button navDashboardBtn;

    // المبيعات
    @FXML private TitledPane salesPane;
    @FXML private Button posBtn;
    @FXML private Button salesListBtn;
    @FXML private Button patientReturnsBtn;
    @FXML private Button localCustomersBtn;
    @FXML private Button insuranceBtn;

    // المشتريات
    @FXML private TitledPane purchasesPane;
    @FXML private Button newPurchaseBtn;
    @FXML private Button purchasesListBtn;
    @FXML private Button suppliersBtn;
    @FXML private Button supplierReturnsBtn;

    // المستودع
    @FXML private TitledPane inventoryPane;
    @FXML private Button medicinesBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button batchesBtn;
    @FXML private Button alternativesBtn;
    @FXML private Button adjustmentsBtn;
    
    // المالية
    @FXML private TitledPane financePane;
    @FXML private Button shiftsBtn;
    @FXML private Button expensesBtn;
    @FXML private Button customerReceiptsBtn;
    @FXML private Button insurancePaymentsBtn;
    @FXML private Button supplierPaymentsBtn;

    // التقارير والكشوفات
    @FXML private TitledPane reportsPane;
    @FXML private Button masterFinancialReportBtn;
    @FXML private Button customerStatementBtn;
    @FXML private Button insuranceStatementBtn;
    @FXML private Button supplierStatementBtn;

    // الإدارة
    @FXML private TitledPane adminPane;
    @FXML private Button usersBtn;
    @FXML private Button rolesBtn;
    @FXML private Button auditLogsBtn;
    @FXML private Button settingsBtn;

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn()) {
            applyPermissions(session);
        }
    }

    private void applyPermissions(SessionManager session) {
        
        // 1. الواجهة الرئيسية
        setNodeVisibility(navDashboardBtn, session.hasPermission("dashboard_view_analytics"));

        // 2. المبيعات
        setNodeVisibility(posBtn, session.hasPermission("pos_create_sale"));
        setNodeVisibility(salesListBtn, session.hasPermission("pos_view_all_sales") || session.hasPermission("pos_view_own_sales"));
        setNodeVisibility(patientReturnsBtn, session.hasPermission("pos_process_return"));
        setNodeVisibility(localCustomersBtn, session.hasPermission("pos_manage_customers"));
        setNodeVisibility(insuranceBtn, session.hasPermission("pos_manage_insurance"));
        checkPaneVisibility(salesPane, posBtn, salesListBtn, patientReturnsBtn, localCustomersBtn, insuranceBtn);

        // 3. المشتريات
        setNodeVisibility(newPurchaseBtn, session.hasPermission("purchasing_create_invoice"));
        setNodeVisibility(purchasesListBtn, session.hasPermission("purchasing_view_all_invoices") || session.hasPermission("purchasing_view_own_invoices"));
        setNodeVisibility(suppliersBtn, session.hasPermission("purchasing_manage_suppliers"));
        setNodeVisibility(supplierReturnsBtn, session.hasPermission("purchasing_process_return"));
        checkPaneVisibility(purchasesPane, newPurchaseBtn, purchasesListBtn, suppliersBtn, supplierReturnsBtn);

        // 4. المستودع
        setNodeVisibility(medicinesBtn, session.hasPermission("inventory_manage_medicines") || session.hasPermission("inventory_view"));
        setNodeVisibility(categoriesBtn, session.hasPermission("inventory_manage_categories"));
        setNodeVisibility(batchesBtn, session.hasPermission("inventory_view"));
        setNodeVisibility(alternativesBtn, session.hasPermission("inventory_manage_medicines"));
        setNodeVisibility(adjustmentsBtn, session.hasPermission("inventory_adjust_stock"));
        checkPaneVisibility(inventoryPane, medicinesBtn, categoriesBtn, batchesBtn, alternativesBtn, adjustmentsBtn);

        // 5. المالية
        setNodeVisibility(shiftsBtn, session.hasPermission("finance_view_all_shifts") || session.hasPermission("finance_view_own_shifts"));
        setNodeVisibility(expensesBtn, session.hasPermission("finance_manage_expenses"));
        setNodeVisibility(customerReceiptsBtn, session.hasPermission("finance_receive_payments"));
        setNodeVisibility(insurancePaymentsBtn, session.hasPermission("finance_receive_payments")); 
        setNodeVisibility(supplierPaymentsBtn, session.hasPermission("finance_make_payments"));
        checkPaneVisibility(financePane, shiftsBtn, expensesBtn, customerReceiptsBtn, insurancePaymentsBtn, supplierPaymentsBtn);

        // 6. التقارير والكشوفات
        setNodeVisibility(masterFinancialReportBtn, session.hasPermission("reports_view_master")); // صلاحية خاصة للمدير
        setNodeVisibility(customerStatementBtn, session.hasPermission("reports_view_finance")); 
        setNodeVisibility(insuranceStatementBtn, session.hasPermission("reports_view_finance"));
        setNodeVisibility(supplierStatementBtn, session.hasPermission("reports_view_finance")); 
        checkPaneVisibility(reportsPane, masterFinancialReportBtn, customerStatementBtn, insuranceStatementBtn, supplierStatementBtn); 

        // 7. الإدارة
        setNodeVisibility(usersBtn, session.hasPermission("admin_manage_users"));
        setNodeVisibility(rolesBtn, session.hasPermission("admin_manage_roles"));
        setNodeVisibility(auditLogsBtn, session.hasPermission("audit_view_logs"));
        setNodeVisibility(settingsBtn, session.hasPermission("admin_manage_settings"));
        checkPaneVisibility(adminPane, usersBtn, rolesBtn, auditLogsBtn, settingsBtn);
    }

    private void setNodeVisibility(Node node, boolean isVisible) {
        node.setVisible(isVisible);
        node.setManaged(isVisible);
    }

    private void checkPaneVisibility(TitledPane pane, Node... nodes) {
        boolean hasVisibleChild = false;
        for (Node node : nodes) {
            if (node.isVisible()) {
                hasVisibleChild = true;
                break;
            }
        }
        setNodeVisibility(pane, hasVisibleChild);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // إظهار رسالة بسيطة جداً
        boolean confirm = AlertManager.showConfirmation("تسجيل الخروج", "هل أنت متأكد من تسجيل الخروج؟");
        
        if (confirm) {
            // تسجيل خروج المستخدم فقط (الوردية تبقى مفتوحة كما هي)
            SessionManager.getInstance().logout(); 
            
            // استخراج النافذة الحالية وتمريرها لمدير الواجهات للعودة لشاشة الدخول
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            ViewManager.getInstance().openLoginWindow(stage);
        }
    }

    // ==========================================
    // دوال التنقل
    // ==========================================

    @FXML private void navDashboard(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/dashboard/DashboardView.fxml"); }
    
    @FXML private void navPOS(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/pos/PosMainView.fxml"); }
    @FXML private void navSales(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/pos/SalesManagementView.fxml"); }
    @FXML private void navPatientReturns(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/pos/PatientReturnsView.fxml"); }
    @FXML private void navLocalCustomers(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/pos/LocalCustomersView.fxml"); }
    @FXML private void navInsurance(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/pos/InsuranceCompaniesView.fxml"); }
    
    @FXML private void navNewPurchase(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/purchases/PurchaseMainView.fxml"); }
    @FXML private void navPurchasesList(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/purchases/PurchaseHistoryView.fxml"); }
    @FXML private void navSuppliers(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/purchases/SupplierMainView.fxml"); }
    @FXML private void navSupplierReturns(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/purchases/SupplierReturnsView.fxml"); }
    
    @FXML private void navMedicines(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/inventory/MedicineMainView.fxml"); }
    @FXML private void navCategories(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/inventory/MedicineCategoryView.fxml"); }
    @FXML private void navBatches(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/inventory/BatchMainView.fxml"); }
    @FXML private void navAlternatives(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/inventory/MedicineAlternativesView.fxml"); }
    @FXML private void navAdjustments(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/inventory/InventoryAdjustmentMainView.fxml"); }
    
    @FXML private void navShifts(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/ShiftManagementView.fxml");  }
    @FXML private void navExpenses(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/ExpenseView.fxml"); }
    @FXML private void navCustomerReceipts(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/CustomerPaymentsView.fxml");  }
    @FXML private void navInsurancePayments(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/InsurancePaymentsView.fxml"); }
    @FXML private void navSupplierPayments(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/purchases/SupplierPaymentsView.fxml"); }
    
    // --- دوال التقارير (تم تصحيحها لتعمل مع الـ Views الصحيحة التي بنيناها) ---
    @FXML private void navMasterFinancialReport(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/reports/FinancialReportMainView.fxml"); }
    @FXML private void navCustomerStatement(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/CustomerStatementView.fxml"); }
    @FXML private void navInsuranceStatement(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/finance/InsuranceStatementView.fxml");   }
    @FXML private void navSupplierStatement(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/purchases/SupplierStatementView.fxml"); } 
    
    @FXML private void navUsers(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/security/UserMainView.fxml"); }
    @FXML private void navRoles(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/security/RoleMainView.fxml"); }
    @FXML private void navAuditLogs(ActionEvent event) {  ViewManager.getInstance().switchScene("/com/pharmacy/views/system/AuditLogMainView.fxml");  }
    @FXML private void navSettings(ActionEvent event) { ViewManager.getInstance().switchScene("/com/pharmacy/views/system/SystemSettingsView.fxml"); }
}