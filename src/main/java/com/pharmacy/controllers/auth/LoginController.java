// المسار: src/main/java/com/pharmacy/controllers/auth/LoginController.java

package com.pharmacy.controllers.auth;

import com.pharmacy.dao.impl.security.PermissionDAOImpl;
import com.pharmacy.dao.impl.security.RoleDAOImpl;
import com.pharmacy.dao.impl.security.RolePermissionDAOImpl;
import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.dao.impl.security.ShiftDAOImpl;
import com.pharmacy.models.security.Permission;
import com.pharmacy.models.security.Shift;
import com.pharmacy.models.security.User;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.impl.security.RoleServiceImpl;
import com.pharmacy.services.impl.security.ShiftServiceImpl;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.interfaces.security.RoleService;
import com.pharmacy.services.interfaces.security.ShiftService;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.utils.exceptions.AuthenticationException;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private UserService userService;
    private RoleService roleService;
    private ShiftService shiftService; 

    @FXML
    public void initialize() {
        this.userService = new UserServiceImpl(new UserDAOImpl());
        this.roleService = new RoleServiceImpl(new RoleDAOImpl(), new PermissionDAOImpl(), new RolePermissionDAOImpl());
        this.shiftService = new ShiftServiceImpl(new ShiftDAOImpl());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        errorLabel.setVisible(false);
        errorLabel.setText("");

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showError("الرجاء إدخال اسم المستخدم وكلمة المرور");
            return;
        }

        try {
            // 1. المصادقة
            User loggedInUser = userService.login(username.trim(), password);

            // 2. جلب وتجهيز الصلاحيات
            List<Permission> rolePermissions = roleService.getPermissionsForRole(loggedInUser.getRole_id());
            Set<String> permissionsSet = rolePermissions.stream()
                    .map(Permission::getPerm_name)
                    .collect(Collectors.toSet());

            // 3. إنشاء الجلسة
            SessionManager.getInstance().startSession(loggedInUser, permissionsSet);

            // 4. استعادة الوردية المفتوحة إن وجدت
            Optional<Shift> openShift = shiftService.getCurrentOpenShift(loggedInUser.getUser_id());
            if (openShift.isPresent()) {
                SessionManager.getInstance().setCurrentShift(openShift.get());
                System.out.println("تم تحميل الوردية المفتوحة بنجاح.");
            } else {
                System.out.println("تنبيه: لا توجد وردية مفتوحة حالياً لهذا المستخدم.");
            }

            // 5. الانتقال إلى النافذة الرئيسية
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            ViewManager.getInstance().changeRootWindow("/com/pharmacy/views/layouts/MainLayout.fxml", stage);

            // 6. التوجيه الذكي بناءً على الصلاحيات
            if (SessionManager.getInstance().hasPermission("dashboard_view_analytics")) {
                ViewManager.getInstance().switchScene("/com/pharmacy/views/dashboard/DashboardView.fxml");
            } else if (SessionManager.getInstance().hasPermission("pos_create_sale")) {
                ViewManager.getInstance().switchScene("/com/pharmacy/views/pos/PosMainView.fxml");
            } else {
                System.out.println("لا يملك المستخدم صلاحية واضحة للرئيسية أو المبيعات.");
            }

            // 🚀 الحل الجذري: إعادة إجبار النافذة على وضع ملء الشاشة بعد تحميل الواجهات الجديدة تماماً
            stage.setMaximized(true);

        } catch (AuthenticationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ في النظام", "حدث خطأ غير متوقع أثناء محاولة تسجيل الدخول. يرجى مراجعة السجلات.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}