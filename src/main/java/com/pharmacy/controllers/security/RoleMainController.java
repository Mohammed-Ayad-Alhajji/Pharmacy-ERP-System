package com.pharmacy.controllers.security;

import com.pharmacy.dao.impl.security.PermissionDAOImpl;
import com.pharmacy.dao.impl.security.RoleDAOImpl;
import com.pharmacy.dao.impl.security.RolePermissionDAOImpl;
import com.pharmacy.models.security.Permission;
import com.pharmacy.models.security.Role;
import com.pharmacy.services.impl.security.RoleServiceImpl;
import com.pharmacy.services.interfaces.security.RoleService;
import com.pharmacy.utils.exceptions.SecurityException;
import com.pharmacy.utils.gui.AlertManager;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoleMainController {

    @FXML private ListView<Role> rolesListView;
    @FXML private TextField roleNameField;
    @FXML private TextField roleDescField;
    @FXML private FlowPane permissionsFlowPane;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;

    private RoleService roleService;
    private List<CheckBox> permissionCheckBoxes = new ArrayList<>();
    private Role currentRole; 

    @FXML
    public void initialize() {
        roleService = new RoleServiceImpl(new RoleDAOImpl(), new PermissionDAOImpl(), new RolePermissionDAOImpl());

        setupRolesListView();
        buildPermissionsUI(); 
        
        loadRoles();
        handleAddNewRole(null);
    }

    private void setupRolesListView() {
        rolesListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("🛡️ " + item.getRole_name());
                    setFont(Font.font("System", FontWeight.BOLD, 14));
                }
            }
        });

        rolesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateRoleDetails(newSelection);
            }
        });
    }

    private void loadRoles() {
        List<Role> roles = roleService.getAllRoles();
        rolesListView.setItems(FXCollections.observableArrayList(roles));
    }

    private void buildPermissionsUI() {
        permissionsFlowPane.getChildren().clear();
        permissionCheckBoxes.clear();

        List<Permission> allPermissions = roleService.getAllPermissions();
        
        // تجميع الصلاحيات حسب الوحدة (module) مع ترجمة اسم الوحدة
        Map<String, List<Permission>> groupedPermissions = allPermissions.stream()
                .collect(Collectors.groupingBy(p -> getArabicName(p.getModule() != null ? p.getModule() : "أخرى")));

        for (Map.Entry<String, List<Permission>> entry : groupedPermissions.entrySet()) {
            VBox moduleCard = new VBox(10);
            moduleCard.setPadding(new Insets(15));
            moduleCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8;");
            moduleCard.setPrefWidth(240); 

            Label moduleLabel = new Label("📦 " + entry.getKey());
            moduleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
            moduleLabel.setStyle("-fx-text-fill: #3b82f6;");
            moduleCard.getChildren().add(moduleLabel);

            for (Permission perm : entry.getValue()) {
                // ترجمة اسم الصلاحية للعربية ليقرأها المدير
                CheckBox cb = new CheckBox(getArabicName(perm.getPerm_name()));
                cb.setFont(Font.font("System", 13));
                cb.setUserData(perm.getPerm_id()); 
                cb.setWrapText(true);
                
                permissionCheckBoxes.add(cb); 
                moduleCard.getChildren().add(cb);
            }

            permissionsFlowPane.getChildren().add(moduleCard);
        }
    }

    // --- دالة الترجمة البسيطة (لا تؤثر على قاعدة البيانات) ---
    private String getArabicName(String key) {
        if (key == null) return "غير محدد";
        switch (key) {
            // ترجمة الأقسام (Modules)
            case "dashboard": return "الواجهة الرئيسية";
            case "pos": return "المبيعات";
            case "purchasing": return "المشتريات";
            case "inventory": return "المستودع";
            case "finance": return "المالية";
            case "reports": return "التقارير";
            case "admin": return "الإدارة";
            case "audit": return "سجلات النظام";
            
            // ترجمة الصلاحيات (Permissions) كما هي في السايد بار
            case "dashboard_view_analytics": return "رؤية التحليلات والإحصائيات";
            
            case "pos_create_sale": return "إنشاء فاتورة مبيعات جديدة";
            case "pos_view_all_sales": return "عرض جميع المبيعات";
            case "pos_view_own_sales": return "عرض مبيعاته فقط";
            case "pos_process_return": return "إجراء مرتجع مبيعات";
            case "pos_manage_customers": return "إدارة العملاء المحليين";
            case "pos_manage_insurance": return "إدارة شركات التأمين";
            
            case "purchasing_create_invoice": return "إنشاء فاتورة مشتريات";
            case "purchasing_view_all_invoices": return "عرض جميع فواتير المشتريات";
            case "purchasing_view_own_invoices": return "عرض فواتير مشترياته فقط";
            case "purchasing_manage_suppliers": return "إدارة الموردين";
            case "purchasing_process_return": return "إجراء مرتجع للمورد";
            
            case "inventory_manage_medicines": return "إدارة بيانات الأدوية";
            case "inventory_view": return "استعراض الأدوية والتشغيلات";
            case "inventory_manage_categories": return "إدارة تصنيفات الأدوية";
            case "inventory_adjust_stock": return "تسوية وتعديل المخزون";
            
            case "finance_view_all_shifts": return "عرض جميع الورديات المالية";
            case "finance_view_own_shifts": return "عرض وردياته المالية فقط";
            case "finance_manage_expenses": return "إدارة المصروفات";
            case "finance_receive_payments": return "استلام دفعات (سند قبض)";
            case "finance_make_payments": return "دفع أموال (سند صرف)";
            
            case "reports_view_master": return "عرض التقرير المالي الشامل (المدير)";
            case "reports_view_finance": return "عرض كشوفات الحساب";
            
            case "admin_manage_users": return "إدارة مستخدمي النظام";
            case "admin_manage_roles": return "إدارة الأدوار والصلاحيات";
            case "admin_manage_settings": return "تعديل الإعدادات العامة";
            case "audit_view_logs": return "استعراض سجلات المراقبة";
            
            default: return key; // إذا لم تجد ترجمة، اعرض الكلمة كما هي
        }
    }
    // --------------------------------------------------------

    private void populateRoleDetails(Role role) {
        this.currentRole = role;
        roleNameField.setText(role.getRole_name());
        roleDescField.setText(role.getDescription());
        
        boolean isSuperUser = role.getRole_id() == 1; 
        deleteBtn.setDisable(isSuperUser);
        saveBtn.setDisable(isSuperUser);
        roleNameField.setDisable(isSuperUser);
        roleDescField.setDisable(isSuperUser);
        permissionCheckBoxes.forEach(cb -> cb.setDisable(isSuperUser));

        permissionCheckBoxes.forEach(cb -> cb.setSelected(false));

        List<Permission> rolePermissions = roleService.getPermissionsForRole(role.getRole_id());
        List<Integer> rolePermIds = rolePermissions.stream().map(Permission::getPerm_id).collect(Collectors.toList());

        for (CheckBox cb : permissionCheckBoxes) {
            if (rolePermIds.contains((Integer) cb.getUserData())) {
                cb.setSelected(true);
            }
        }
    }

    @FXML
    private void handleAddNewRole(ActionEvent event) {
        rolesListView.getSelectionModel().clearSelection();
        currentRole = null;
        
        roleNameField.clear();
        roleDescField.clear();
        roleNameField.setDisable(false);
        roleDescField.setDisable(false);
        saveBtn.setDisable(false);
        deleteBtn.setDisable(true); 
        
        permissionCheckBoxes.forEach(cb -> {
            cb.setSelected(false);
            cb.setDisable(false);
        });
    }

    @FXML
    private void handleSaveRole(ActionEvent event) {
        String name = roleNameField.getText().trim();
        if (name.isEmpty()) {
            AlertManager.showWarning("بيانات ناقصة", "يرجى إدخال اسم الدور الوظيفي.");
            return;
        }

        try {
            boolean isNew = (currentRole == null);
            Role roleToSave = isNew ? new Role() : currentRole;
            roleToSave.setRole_name(name);
            roleToSave.setDescription(roleDescField.getText().trim());

            if (isNew) {
                roleService.createRole(roleToSave);
            } else {
                roleService.updateRole(roleToSave);
            }

            List<Integer> selectedPermIds = permissionCheckBoxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (Integer) cb.getUserData())
                    .collect(Collectors.toList());

            roleService.updateRolePermissions(roleToSave.getRole_id(), selectedPermIds);

            AlertManager.showSuccess("نجاح", "تم حفظ بيانات الدور وصلاحياته بنجاح.");
            loadRoles(); 
            rolesListView.getSelectionModel().select(roleToSave); 
            
        } catch (SecurityException e) {
            AlertManager.showError("خطأ", e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ", "حدث خطأ غير متوقع: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteRole(ActionEvent event) {
        if (currentRole == null) return;

        if (AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف دور '" + currentRole.getRole_name() + "' نهائياً؟")) {
            try {
                roleService.deleteRole(currentRole.getRole_id());
                AlertManager.showSuccess("نجاح", "تم حذف الدور بنجاح.");
                loadRoles();
                handleAddNewRole(null); 
            } catch (SecurityException e) {
                AlertManager.showError("حذف مرفوض", e.getMessage());
            }
        }
    }
}