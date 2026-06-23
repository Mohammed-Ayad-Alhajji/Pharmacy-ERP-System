package com.pharmacy.controllers.system;

import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.dao.impl.system.AuditLogDAOImpl;
import com.pharmacy.models.security.User;
import com.pharmacy.models.system.AuditLog;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.impl.system.AuditLogServiceImpl;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.services.interfaces.system.AuditLogService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditLogController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> actionFilterCombo;
    @FXML private ComboBox<String> tableFilterCombo;

    @FXML private TableView<AuditLog> logsTable;
    @FXML private TableColumn<AuditLog, String> idCol;
    @FXML private TableColumn<AuditLog, String> dateCol;
    @FXML private TableColumn<AuditLog, String> userCol;
    @FXML private TableColumn<AuditLog, String> actionCol;
    @FXML private TableColumn<AuditLog, String> tableCol;

    @FXML private Pagination pagination;
    @FXML private TextArea oldDataArea;
    @FXML private TextArea newDataArea;

    private AuditLogService auditLogService;
    private UserService userService;

    private ObservableList<AuditLog> masterLogsList = FXCollections.observableArrayList();
    private FilteredList<AuditLog> filteredLogs;
    private Map<Integer, String> usersCache = new HashMap<>(); 
    
    private final int ROWS_PER_PAGE = 30; 

    @FXML
    public void initialize() {
        auditLogService = new AuditLogServiceImpl(new AuditLogDAOImpl());
        userService = new UserServiceImpl(new UserDAOImpl());

        // تعبئة كاش المستخدمين
        List<User> allUsers = userService.getUsers(true, 1000, 0);
        for (User u : allUsers) {
            usersCache.put(u.getUser_id(), u.getFull_name());
        }

        setupFilters();
        setupTable();
        
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
        
        loadDataFromDatabase();
    }

    // ==========================================
    // دوال الترجمة (للمدير والمبرمج معاً)
    // ==========================================
    private String translateAction(String action) {
        if (action == null) return "غير معروف";
        switch (action.toUpperCase()) {
            case "INSERT": return "إضافة سجل جديد (INSERT)";
            case "UPDATE": return "تعديل بيانات (UPDATE)";
            case "DELETE": return "حذف بيانات (DELETE)";
            case "DEACTIVATE": return "إيقاف/تعطيل (DEACTIVATE)";
            case "UPDATE_PRICE": return "تغيير أسعار (UPDATE_PRICE)";
            case "LOGIN": return "تسجيل دخول (LOGIN)";
            case "LOGOUT": return "تسجيل خروج (LOGOUT)";
            default: return action;
        }
    }

    private String translateTable(String table) {
        if (table == null) return "غير معروف";
        switch (table) {
            case "Users": return "الموظفين والمستخدمين (Users)";
            case "Roles": return "الأدوار والصلاحيات (Roles)";
            case "Medicines": return "دليل الأدوية (Medicines)";
            case "Batches": return "تشغيلات المستودع (Batches)";
            case "Purchases": return "فواتير المشتريات (Purchases)";
            case "Sales": return "فواتير المبيعات (Sales)";
            case "System_Settings": return "إعدادات النظام (Settings)";
            case "Inventory_Adjustments": return "تسويات الجرد (Adjustments)";
            default: return table;
        }
    }

    private void setupFilters() {
        actionFilterCombo.setItems(FXCollections.observableArrayList(
                "الكل", "INSERT", "UPDATE", "DELETE", "DEACTIVATE", "UPDATE_PRICE", "LOGIN", "LOGOUT"
        ));
        
        // عرض النصوص العربية في القائمة المنسدلة للحركات
        actionFilterCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String object) {
                if ("الكل".equals(object)) return "جميع الحركات";
                return translateAction(object);
            }
            @Override
            public String fromString(String string) { return string; }
        });
        actionFilterCombo.getSelectionModel().selectFirst();

        tableFilterCombo.setItems(FXCollections.observableArrayList(
                "الكل", "Users", "Roles", "Medicines", "Batches", "Purchases", "Sales", "System_Settings", "Inventory_Adjustments"
        ));
        
        // عرض النصوص العربية في القائمة المنسدلة للجداول
        tableFilterCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String object) {
                if ("الكل".equals(object)) return "جميع السجلات والمقاطع";
                return translateTable(object);
            }
            @Override
            public String fromString(String string) { return string; }
        });
        tableFilterCombo.getSelectionModel().selectFirst();

        actionFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyLocalFilter());
        tableFilterCombo.valueProperty().addListener((obs, oldV, newV) -> applyLocalFilter());
        
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> loadDataFromDatabase());
        endDatePicker.valueProperty().addListener((obs, oldV, newV) -> loadDataFromDatabase());
    }

    private void loadDataFromDatabase() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        
        if (start != null && end != null) {
            LocalDateTime startDT = start.atStartOfDay();
            LocalDateTime endDT = end.atTime(23, 59, 59);
            List<AuditLog> dbLogs = auditLogService.getLogsByDateRange(startDT, endDT, 1000, 0);
            masterLogsList.setAll(dbLogs);
            applyLocalFilter();
        }
    }

    private void applyLocalFilter() {
        filteredLogs = new FilteredList<>(masterLogsList, log -> {
            String actionFilter = actionFilterCombo.getValue();
            if (!"الكل".equals(actionFilter) && !log.getAction_type().equalsIgnoreCase(actionFilter)) {
                return false;
            }

            String tableFilter = tableFilterCombo.getValue();
            if (!"الكل".equals(tableFilter) && !log.getTable_affected().equalsIgnoreCase(tableFilter)) {
                return false;
            }
            return true;
        });

        updatePagination();
    }

    private void setupTable() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getLog_id())));
        
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAction_timestamp() != null ? c.getValue().getAction_timestamp().format(dtf) : "غير محدد"
        ));

        userCol.setCellValueFactory(c -> {
            Integer uid = c.getValue().getUser_id();
            if (uid == null) return new SimpleStringProperty("النظام (System)");
            return new SimpleStringProperty(usersCache.getOrDefault(uid, "مستخدم مجهول (" + uid + ")"));
        });

        // استخدام دالة الترجمة للجدول المتأثر
        tableCol.setCellValueFactory(c -> new SimpleStringProperty(translateTable(c.getValue().getTable_affected())));

        // استخدام دالة الترجمة لنوع الحركة مع التلوين
        actionCol.setCellValueFactory(c -> new SimpleStringProperty(translateAction(c.getValue().getAction_type())));
        actionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold;");
                    // التلوين بناءً على الكلمة الإنجليزية الموجودة بين الأقواس
                    if (item.contains("DELETE") || item.contains("DEACTIVATE")) {
                        setTextFill(javafx.scene.paint.Color.valueOf("#ef4444")); // أحمر
                    } else if (item.contains("INSERT")) {
                        setTextFill(javafx.scene.paint.Color.valueOf("#22c55e")); // أخضر
                    } else if (item.contains("UPDATE") || item.contains("UPDATE_PRICE")) {
                        setTextFill(javafx.scene.paint.Color.valueOf("#f59e0b")); // برتقالي
                    } else {
                        setTextFill(javafx.scene.paint.Color.valueOf("#3b82f6")); // أزرق
                    }
                }
            }
        });

        logsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                oldDataArea.setText(newSel.getOld_data() != null ? newSel.getOld_data().replace(" | ", "\n") : "لا توجد بيانات قديمة لعرضها.");
                newDataArea.setText(newSel.getNew_data() != null ? newSel.getNew_data().replace(" | ", "\n") : "لا توجد بيانات جديدة لعرضها.");
            } else {
                oldDataArea.clear();
                newDataArea.clear();
            }
        });
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) filteredLogs.size() / ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> 
            updateTablePage(newIndex.intValue())
        );
        
        updateTablePage(0);
    }

    private void updateTablePage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredLogs.size());
        
        if (fromIndex < toIndex) {
            logsTable.setItems(FXCollections.observableArrayList(filteredLogs.subList(fromIndex, toIndex)));
        } else {
            logsTable.setItems(FXCollections.observableArrayList());
        }
    }

    @FXML
    private void handleClearFilters(ActionEvent event) {
        actionFilterCombo.getSelectionModel().selectFirst();
        tableFilterCombo.getSelectionModel().selectFirst();
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
    }
}