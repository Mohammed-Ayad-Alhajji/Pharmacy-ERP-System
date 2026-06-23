// المسار: src/main/java/com/pharmacy/controllers/finance/ExpenseMainController.java

package com.pharmacy.controllers.finance;

import com.pharmacy.dao.impl.finance.ExpenseCategoryDAOImpl;
import com.pharmacy.dao.impl.finance.ExpenseDAOImpl;
import com.pharmacy.dao.impl.system.AuditLogDAOImpl;
import com.pharmacy.models.finance.Expense;
import com.pharmacy.models.finance.ExpenseCategory;
import com.pharmacy.models.security.Shift;
import com.pharmacy.models.system.AuditLog;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.impl.finance.ExpenseCategoryServiceImpl;
import com.pharmacy.services.impl.finance.ExpenseServiceImpl;
import com.pharmacy.services.interfaces.finance.ExpenseCategoryService;
import com.pharmacy.services.interfaces.finance.ExpenseService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.ViewManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExpenseMainController {

    @FXML private Button manageCategoriesBtn;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private MenuButton categoryFilterMenu;
    @FXML private Label totalExpensesLabel;

    @FXML private TableView<Expense> expensesTable;
    @FXML private TableColumn<Expense, Integer> idCol;
    @FXML private TableColumn<Expense, String> dateCol;
    @FXML private TableColumn<Expense, String> categoryCol;
    @FXML private TableColumn<Expense, BigDecimal> amountCol;
    @FXML private TableColumn<Expense, String> descCol;
    @FXML private TableColumn<Expense, Void> actionCol;

    // --- عناصر نظام الصفحات ---
    @FXML private ComboBox<Integer> rowsPerPageCombo;
    @FXML private Pagination pagination;

    private ExpenseService expenseService;
    private ExpenseCategoryService categoryService;
    private AuditLogDAOImpl auditLogDAO;
    
    // --- قوائم البيانات ---
    private ObservableList<Expense> masterDataList = FXCollections.observableArrayList();
    private FilteredList<Expense> filteredData;
    
    private List<ExpenseCategory> allCategories = new ArrayList<>();
    private List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private boolean isManagerOrAccountant = false;

    @FXML
    public void initialize() {
        expenseService = new ExpenseServiceImpl(new ExpenseDAOImpl());
        categoryService = new ExpenseCategoryServiceImpl(new ExpenseCategoryDAOImpl());
        auditLogDAO = new AuditLogDAOImpl();

        isManagerOrAccountant = SessionManager.getInstance().hasPermission("finance_view_all_shifts") || 
                                SessionManager.getInstance().hasPermission("reports_view_finance");

        setupPermissions();
        setupTableColumns();
        setupPaginationControl(); // تهيئة الصفحات
        loadCategoriesIntoFilter();
        
        loadExpenses();
    }

    private void setupPermissions() {
        manageCategoriesBtn.setVisible(isManagerOrAccountant);
        manageCategoriesBtn.setManaged(isManagerOrAccountant);

        if (!isManagerOrAccountant) {
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setValue(LocalDate.now());
            startDatePicker.setDisable(true);
            endDatePicker.setDisable(true);
        } else {
            startDatePicker.setValue(LocalDate.now().minusDays(7));
            endDatePicker.setValue(LocalDate.now());
        }
    }

    private void loadCategoriesIntoFilter() {
        allCategories = categoryService.getAllCategories();
        categoryFilterMenu.getItems().clear();
        categoryCheckBoxes.clear();

        for (ExpenseCategory cat : allCategories) {
            CheckBox cb = new CheckBox(cat.getName());
            cb.setOnAction(e -> updateCategoryMenuText());
            categoryCheckBoxes.add(cb);
            
            CustomMenuItem menuItem = new CustomMenuItem(cb);
            menuItem.setHideOnClick(false); 
            categoryFilterMenu.getItems().add(menuItem);
        }
        updateCategoryMenuText();
    }

    private void updateCategoryMenuText() {
        long selectedCount = categoryCheckBoxes.stream().filter(CheckBox::isSelected).count();
        if (selectedCount == 0 || selectedCount == categoryCheckBoxes.size()) {
            categoryFilterMenu.setText("كل الفئات");
        } else {
            categoryFilterMenu.setText(selectedCount + " فئات محددة");
        }
    }

    private void setupTableColumns() {
        // الترقيم التسلسلي المستمر بدلاً من ID قاعدة البيانات
        idCol.setCellValueFactory(param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(null));
        idCol.setCellFactory(col -> new TableCell<Expense, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                } else {
                    int currentPage = pagination != null ? pagination.getCurrentPageIndex() : 0;
                    int rowsPerPage = (rowsPerPageCombo != null && rowsPerPageCombo.getValue() != null) ? rowsPerPageCombo.getValue() : 20;
                    int offset = currentPage * rowsPerPage;
                    setText(String.valueOf(offset + getIndex() + 1));
                }
            }
        });

        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        dateCol.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getExpense_date();
            return new SimpleStringProperty(date != null ? date.format(formatter) : "");
        });

        categoryCol.setCellValueFactory(cellData -> {
            int catId = cellData.getValue().getCategory_id();
            Optional<ExpenseCategory> cat = allCategories.stream().filter(c -> c.getCategory_id() == catId).findFirst();
            return new SimpleStringProperty(cat.isPresent() ? cat.get().getName() : "غير معروف");
        });

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("تعديل ✏️");
            private final Button deleteBtn = new Button("حذف 🗑️");
            private final HBox actionButtons = new HBox(5, editBtn, deleteBtn);

            {
                // إضافة الألوان للأزرار
                editBtn.getStyleClass().add("button-primary");
                deleteBtn.getStyleClass().add("button-danger");

                // حدث زر التعديل (يشير للنافذة الذكية الموحدة الجديدة)
                editBtn.setOnAction(e -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    ViewManager.getInstance().showModalWithData("/com/pharmacy/views/finance/ExpenseFormDialog.fxml", "تعديل المصروف", expense);
                    loadExpenses();
                });

                // حدث زر الحذف
                deleteBtn.setOnAction(e -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleDeleteExpense(expense); 
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Expense expense = getTableView().getItems().get(getIndex());
                    Shift currentShift = SessionManager.getInstance().getCurrentShift();
                    
                    if (!isManagerOrAccountant) {
                        if (currentShift == null || expense.getShift_id() != currentShift.getShift_id()) {
                            editBtn.setDisable(true);
                            deleteBtn.setDisable(true);
                        } else {
                            editBtn.setDisable(false);
                            deleteBtn.setDisable(false);
                        }
                    } else {
                        editBtn.setDisable(false);
                        deleteBtn.setDisable(false);
                    }
                    setGraphic(actionButtons);
                }
            }
        });
    }

    // --- إعداد نظام الصفحات ---
    private void setupPaginationControl() {
        rowsPerPageCombo.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        rowsPerPageCombo.setValue(20);
        
        rowsPerPageCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePagination());
        
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updateTableData(newIndex.intValue());
        });
    }

    private void updatePagination() {
        if (filteredData == null) return;
        
        int totalItems = filteredData.size();
        int rowsPerPage = rowsPerPageCombo.getValue() != null ? rowsPerPageCombo.getValue() : 20;
        int pageCount = (int) Math.ceil((double) totalItems / rowsPerPage);
        
        if (pageCount == 0) pageCount = 1;
        
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        updateTableData(0);
    }

    private void updateTableData(int pageIndex) {
        if (filteredData == null) return;
        
        int rowsPerPage = rowsPerPageCombo.getValue() != null ? rowsPerPageCombo.getValue() : 20;
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, filteredData.size());
        
        if (fromIndex < toIndex) {
            expensesTable.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        } else {
            expensesTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void loadExpenses() {
        expensesTable.getItems().clear();
        Shift currentShift = SessionManager.getInstance().getCurrentShift();
        List<Expense> resultList = new ArrayList<>();

        if (!isManagerOrAccountant) {
            if (currentShift != null) {
                resultList = expenseService.getExpensesByShift(currentShift.getShift_id());
            }
        } else {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            if (start != null && end != null) {
                resultList = expenseService.getExpensesByDateRange(start, end);
            } else {
                resultList = expenseService.getExpensesByDateRange(LocalDate.now(), LocalDate.now());
            }
        }

        List<String> selectedCatNames = categoryCheckBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());

        if (!selectedCatNames.isEmpty() && selectedCatNames.size() < allCategories.size()) {
            List<Integer> selectedCatIds = allCategories.stream()
                    .filter(c -> selectedCatNames.contains(c.getName()))
                    .map(ExpenseCategory::getCategory_id)
                    .collect(Collectors.toList());
            resultList.removeIf(e -> !selectedCatIds.contains(e.getCategory_id()));
        }

        masterDataList.setAll(resultList);
        filteredData = new FilteredList<>(masterDataList, p -> true);
        
        updatePagination(); // تحديث الصفحات بناءً على البيانات الجديدة
        calculateTotal();
    }

    @FXML
    private void handleApplyFilters(ActionEvent event) {
        loadExpenses();
    }

    @FXML
    private void handleClearFilters(ActionEvent event) {
        for (CheckBox cb : categoryCheckBoxes) {
            cb.setSelected(false);
        }
        updateCategoryMenuText();
        
        if (isManagerOrAccountant) {
            startDatePicker.setValue(LocalDate.now().minusDays(7));
            endDatePicker.setValue(LocalDate.now());
        }
        loadExpenses();
    }

    @FXML
    private void handleAddNewExpense(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/finance/ExpenseFormDialog.fxml", "تسجيل مصروف جديد", null);
        loadExpenses();
    }

    @FXML
    private void handleManageCategories(ActionEvent event) {
        ViewManager.getInstance().showModal("/com/pharmacy/views/finance/ManageCategoriesDialog.fxml", "إدارة فئات المصروفات");
        loadCategoriesIntoFilter();
        loadExpenses();
    }

    private void handleDeleteExpense(Expense expense) {
        boolean confirm = AlertManager.showConfirmation("تأكيد الحذف", "هل أنت متأكد من إلغاء هذا المصروف؟ سيتم تسجيل هذا الإجراء في سجل النظام.");
        if (confirm) {
            try {
                String oldDataJson = String.format("{\"expense_id\": %d, \"amount\": %.2f, \"shift_id\": %d, \"category_id\": %d}", 
                                      expense.getExpense_id(), expense.getAmount(), expense.getShift_id(), expense.getCategory_id());

                if (expenseService.deleteExpense(expense.getExpense_id())) {
                    int currentUserId = SessionManager.getInstance().getCurrentUser().getUser_id();
                    AuditLog logEntry = new AuditLog(
                            currentUserId, 
                            "DELETE", 
                            "Expenses", 
                            oldDataJson, 
                            null, 
                            LocalDateTime.now()
                    );
                    auditLogDAO.create(logEntry);

                    loadExpenses();
                    AlertManager.showSuccess("تم الحذف", "تم حذف المصروف وتوثيق الإجراء بنجاح في سجلات النظام.");
                } else {
                    AlertManager.showError("خطأ", "فشل في عملية الحذف. تأكد من استعلام SQL.");
                }
            } catch (Exception e) {
                System.err.println("حدث خطأ أثناء محاولة الحذف: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void calculateTotal() {
        if (filteredData == null) return;
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : filteredData) {
            total = total.add(e.getAmount());
        }
        totalExpensesLabel.setText(String.format("%.2f", total));
    }
}