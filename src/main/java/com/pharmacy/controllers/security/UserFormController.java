// المسار: src/main/java/com/pharmacy/controllers/security/UserFormController.java

package com.pharmacy.controllers.security;

import com.pharmacy.models.security.Role;
import com.pharmacy.models.security.User;
import com.pharmacy.services.impl.security.RoleServiceImpl;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.interfaces.security.RoleService;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.DataTransferable;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class UserFormController implements DataTransferable {

    @FXML private Label formTitleLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label passwordHintLabel;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private CheckBox activeCheck;
    @FXML private Button saveBtn;

    private UserService userService;
    private RoleService roleService;

    private User currentUser; 
    private String originalUsername; 

    @FXML
    public void initialize() {
        com.pharmacy.dao.impl.security.RoleDAOImpl roleDAO = new com.pharmacy.dao.impl.security.RoleDAOImpl();
        com.pharmacy.dao.impl.security.PermissionDAOImpl permissionDAO = new com.pharmacy.dao.impl.security.PermissionDAOImpl();
        com.pharmacy.dao.impl.security.RolePermissionDAOImpl rolePermissionDAO = new com.pharmacy.dao.impl.security.RolePermissionDAOImpl();
        
        roleService = new RoleServiceImpl(roleDAO, permissionDAO, rolePermissionDAO);
        userService = new UserServiceImpl(new com.pharmacy.dao.impl.security.UserDAOImpl());

        loadRoles();
    }

    private void loadRoles() {
        List<Role> roles = roleService.getAllRoles();
        roleCombo.setItems(FXCollections.observableArrayList(roles));
        
        roleCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getRole_name());
            }
        });
        roleCombo.setButtonCell(roleCombo.getCellFactory().call(null));
    }

    // هنا استخدمنا واجهتك الخاصة DataTransferable
    @Override
    public void receiveData(Object data) {
        if (data instanceof User) {
            this.currentUser = (User) data;
            this.originalUsername = currentUser.getUsername();
            fillFormForEdit();
        } else {
            this.currentUser = null;
            formTitleLabel.setText("إضافة مستخدم جديد 👤");
            passwordHintLabel.setVisible(false); 
        }
    }

    private void fillFormForEdit() {
        formTitleLabel.setText("تعديل بيانات المستخدم ✏️");
        saveBtn.setText("تحديث البيانات 💾");
        passwordHintLabel.setVisible(true); 

        fullNameField.setText(currentUser.getFull_name());
        usernameField.setText(currentUser.getUsername());
        activeCheck.setSelected(currentUser.isActive());

        roleCombo.getItems().stream()
            .filter(role -> role.getRole_id() == currentUser.getRole_id())
            .findFirst()
            .ifPresent(role -> roleCombo.setValue(role));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateInputs()) return;

        try {
            boolean isNewUser = (currentUser == null);
            User userToSave = isNewUser ? new User() : currentUser;

            userToSave.setFull_name(fullNameField.getText().trim());
            userToSave.setUsername(usernameField.getText().trim());
            userToSave.setRole_id(roleCombo.getValue().getRole_id());
            userToSave.setActive(activeCheck.isSelected());

            String plainPassword = passwordField.getText();

            if (isNewUser) {
                if (userService.registerUser(userToSave, plainPassword)) {
                    AlertManager.showSuccess("نجاح", "تمت إضافة المستخدم بنجاح.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "فشل إضافة المستخدم. يرجى المحاولة مرة أخرى.");
                }
            } else {
                String newPassword = (plainPassword != null && !plainPassword.trim().isEmpty()) ? plainPassword : null;
                
                if (userService.updateUser(userToSave, newPassword)) {
                    AlertManager.showSuccess("نجاح", "تم تحديث بيانات المستخدم.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "فشل تحديث البيانات.");
                }
            }

        } catch (Exception e) {
            AlertManager.showError("خطأ", "حدث خطأ أثناء الحفظ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            AlertManager.showWarning("بيانات ناقصة", "يرجى إدخال الاسم الكامل.");
            return false;
        }

        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            AlertManager.showWarning("بيانات ناقصة", "يرجى إدخال اسم الدخول.");
            return false;
        }

        if (roleCombo.getValue() == null) {
            AlertManager.showWarning("بيانات ناقصة", "يرجى اختيار الدور الوظيفي.");
            return false;
        }

        if (currentUser == null) {
            if (passwordField.getText() == null || passwordField.getText().trim().isEmpty()) {
                AlertManager.showWarning("بيانات ناقصة", "يجب تعيين كلمة مرور للمستخدم الجديد.");
                return false;
            }
        }

        String newUsername = usernameField.getText().trim();
        if (currentUser == null || !newUsername.equalsIgnoreCase(originalUsername)) {
            if (userService.isUsernameTaken(newUsername)) {
                AlertManager.showError("خطأ في الإدخال", "اسم الدخول هذا مستخدم بالفعل. يرجى اختيار اسم آخر.");
                return false;
            }
        }

        return true;
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