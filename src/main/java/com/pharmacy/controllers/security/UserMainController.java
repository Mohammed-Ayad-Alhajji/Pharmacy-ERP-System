package com.pharmacy.controllers.security;

import com.pharmacy.dao.impl.security.RoleDAOImpl;
import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.models.security.Role;
import com.pharmacy.models.security.User;
import com.pharmacy.services.impl.security.RoleServiceImpl;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.interfaces.security.RoleService;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserMainController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Role> roleFilterCombo;
    @FXML private CheckBox showInactiveCheckBox;

    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, Integer> indexCol;
    @FXML private TableColumn<UserDTO, String> fullNameCol;
    @FXML private TableColumn<UserDTO, String> usernameCol;
    @FXML private TableColumn<UserDTO, String> roleCol;
    @FXML private TableColumn<UserDTO, String> statusCol;
    @FXML private TableColumn<UserDTO, Void> actionCol;

    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Pagination pagination;

    private UserService userService;
    private RoleService roleService;

    private ObservableList<UserDTO> masterDataList = FXCollections.observableArrayList();
    private FilteredList<UserDTO> filteredData;
    private Map<Integer, Role> rolesCache;

    @FXML
    public void initialize() {
        // تهيئة الـ DAOs الخاصة بالأدوار والصلاحيات أولاً
        com.pharmacy.dao.impl.security.RoleDAOImpl roleDAO = new com.pharmacy.dao.impl.security.RoleDAOImpl();
        com.pharmacy.dao.impl.security.PermissionDAOImpl permissionDAO = new com.pharmacy.dao.impl.security.PermissionDAOImpl();
        com.pharmacy.dao.impl.security.RolePermissionDAOImpl rolePermissionDAO = new com.pharmacy.dao.impl.security.RolePermissionDAOImpl();
        
        // حقن الاعتماديات الصحيح للخدمات
        roleService = new RoleServiceImpl(roleDAO, permissionDAO, rolePermissionDAO);
        userService = new UserServiceImpl(new com.pharmacy.dao.impl.security.UserDAOImpl());

        setupRoleFilter();
        setupTableColumns();
        setupPaginationControl();
        
        loadUsersData();

        // مستمعات الفلاتر
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        roleFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        showInactiveCheckBox.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void setupRoleFilter() {
        List<Role> allRoles = roleService.getAllRoles();
        
        // بناء كاش للوصول السريع للأدوار عند بناء الجدول (O(1) Time Complexity)
        rolesCache = allRoles.stream().collect(Collectors.toMap(Role::getRole_id, r -> r));

        // إعداد القائمة المنسدلة للفلتر
        ObservableList<Role> roleOptions = FXCollections.observableArrayList();
        roleOptions.add(new Role("الكل", "جميع الأدوار")); // خيار افتراضي
        roleOptions.addAll(allRoles);
        
        roleFilterCombo.setItems(roleOptions);
        
        // تخصيص طريقة عرض النص داخل القائمة المنسدلة
        roleFilterCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getRole_name());
            }
        });
        roleFilterCombo.setButtonCell(roleFilterCombo.getCellFactory().call(null));
        roleFilterCombo.getSelectionModel().selectFirst();
    }

    private void loadUsersData() {
        // نجلب جميع المستخدمين (النشطين وغير النشطين) بحد أقصى كبير للفلترة محلياً
        List<User> allUsers = userService.getUsers(true, 10000, 0);

        List<UserDTO> dtoList = allUsers.stream()
                .map(u -> new UserDTO(u, rolesCache.get(u.getRole_id())))
                .collect(Collectors.toList());

        masterDataList.setAll(dtoList);
        applyFilters();
    }

    private void setupTableColumns() {
        indexCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
        indexCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                } else {
                    int currentPage = pagination.getCurrentPageIndex();
                    int rowsPerPage = rowsPerPageCombo.getValue();
                    setText(String.valueOf((currentPage * rowsPerPage) + getIndex() + 1));
                }
            }
        });

        fullNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().getFull_name()));
        usernameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().getUsername()));
        
        roleCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRole() != null ? c.getValue().getRole().getRole_name() : "غير محدد"
        ));

        // تلوين عمود الحالة
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().isActive() ? "نشط 🟢" : "موقوف 🔴"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("موقوف")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // أزرار الإجراءات
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("تعديل ✏️");
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("حذف 🗑️");
            private final HBox actions = new HBox(5, editBtn, toggleBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("button-primary");
                deleteBtn.getStyleClass().add("button-danger");

                editBtn.setOnAction(e -> {
                    UserDTO dto = getTableView().getItems().get(getIndex());
                    ViewManager.getInstance().showModalWithData("/com/pharmacy/views/security/UserFormDialog.fxml", "تعديل بيانات المستخدم", dto.getUser());
                    loadUsersData();
                });

                toggleBtn.setOnAction(e -> handleToggleStatus(getTableView().getItems().get(getIndex()).getUser()));

                deleteBtn.setOnAction(e -> {
                    UserDTO dto = getTableView().getItems().get(getIndex());
                    if (AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف المستخدم '" + dto.getUser().getUsername() + "' نهائياً؟")) {
                        if (userService.deleteUser(dto.getUser().getUser_id())) {
                            AlertManager.showSuccess("نجاح", "تم حذف المستخدم بنجاح.");
                            loadUsersData();
                        } else {
                            AlertManager.showError("خطأ", "لا يمكن حذف المستخدم لارتباطه بعمليات أخرى.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    UserDTO dto = getTableRow().getItem();
                    // تحديث مظهر زر التفعيل/الإيقاف
                    if (dto.getUser().isActive()) {
                        toggleBtn.setText("إيقاف 🛑");
                        toggleBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        toggleBtn.setText("تنشيط ♻️");
                        toggleBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                    }
                    
                    // منع المستخدم من حذف أو إيقاف نفسه بالخطأ (حماية ذاتية)
                    // (بافتراض أن لديك SessionManager يجلب الـ ID الحالي)
                    int currentUserId = com.pharmacy.security.SessionManager.getInstance().getCurrentUser().getUser_id();
                    if (dto.getUser().getUser_id() == currentUserId) {
                        toggleBtn.setDisable(true);
                        deleteBtn.setDisable(true);
                    } else {
                        toggleBtn.setDisable(false);
                        deleteBtn.setDisable(false);
                    }

                    setGraphic(actions);
                }
            }
        });
    }

    private void handleToggleStatus(User user) {
        String action = user.isActive() ? "إيقاف" : "تنشيط";
        if (AlertManager.showConfirmation("تأكيد", "هل تريد " + action + " حساب المستخدم: " + user.getUsername() + "؟")) {
            boolean success = user.isActive() ? userService.deactivateUser(user.getUser_id()) : userService.activateUser(user.getUser_id());
            if (success) {
                loadUsersData(); // إعادة تحميل البيانات لتحديث الجدول
            } else {
                AlertManager.showError("خطأ", "حدث خطأ أثناء تعديل حالة الحساب.");
            }
        }
    }

    private void applyFilters() {
        if (masterDataList == null) return;

        filteredData = new FilteredList<>(masterDataList, dto -> {
            User user = dto.getUser();
            
            // 1. فلتر الحالة (موقوف أم لا)
            if (!showInactiveCheckBox.isSelected() && !user.isActive()) {
                return false;
            }

            // 2. فلتر الدور (Role)
            Role selectedRole = roleFilterCombo.getValue();
            if (selectedRole != null && !"الكل".equals(selectedRole.getRole_name())) {
                if (user.getRole_id() != selectedRole.getRole_id()) {
                    return false;
                }
            }

            // 3. فلتر البحث النصي
            String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            if (search.isEmpty()) return true;

            return (user.getFull_name() != null && user.getFull_name().toLowerCase().contains(search)) ||
                   (user.getUsername() != null && user.getUsername().toLowerCase().contains(search));
        });

        updatePagination();
    }

    private void setupPaginationControl() {
        rowsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        rowsPerPageCombo.setValue(20);
        rowsPerPageCombo.valueProperty().addListener((obs, old, newVal) -> updatePagination());
        pagination.currentPageIndexProperty().addListener((obs, old, newVal) -> updateTableData(newVal.intValue()));
    }

    private void updatePagination() {
        int rowsPerPage = rowsPerPageCombo.getValue();
        int pageCount = (int) Math.ceil((double) filteredData.size() / rowsPerPage);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        updateTableData(0);
    }

    private void updateTableData(int pageIndex) {
        int rowsPerPage = rowsPerPageCombo.getValue();
        int from = pageIndex * rowsPerPage;
        int to = Math.min(from + rowsPerPage, filteredData.size());
        usersTable.setItems(FXCollections.observableArrayList(from < to ? filteredData.subList(from, to) : FXCollections.observableArrayList()));
    }

    @FXML
    private void handleAddNewUser(ActionEvent event) {
        // فتح شاشة إضافة مستخدم جديد (سنقوم بإنشائها في الخطوة التالية)
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/security/UserFormDialog.fxml", "إضافة مستخدم جديد", null);
        loadUsersData(); // تحديث الجدول بعد إغلاق النافذة
    }

    // ==========================================
    // DTO Class لربط المستخدم بالدور الخاص به في الجدول
    // ==========================================
    public static class UserDTO {
        private final User user;
        private final Role role;

        public UserDTO(User user, Role role) {
            this.user = user;
            this.role = role;
        }

        public User getUser() { return user; }
        public Role getRole() { return role; }
    }
}