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
import com.pharmacy.utils.gui.DataTransferable;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExpenseFormController implements DataTransferable {

    @FXML private Label formTitleLabel;
    @FXML private ComboBox<ExpenseCategory> categoryCombo;
    @FXML private TextField amountField;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveBtn;

    private ExpenseService expenseService;
    private ExpenseCategoryService categoryService;
    private AuditLogDAOImpl auditLogDAO;

    private Expense currentExpense; // null = إضافة، غير ذلك = تعديل
    private String oldDataJson; // لتخزين الحالة القديمة للمراقبة (Audit Log)

    @FXML
    public void initialize() {
        expenseService = new ExpenseServiceImpl(new ExpenseDAOImpl());
        categoryService = new ExpenseCategoryServiceImpl(new ExpenseCategoryDAOImpl());
        auditLogDAO = new AuditLogDAOImpl();

        setupSearchableCategoryCombo();
        setupNumericValidation();
        
        // التركيز التلقائي على قائمة الفئات لتسريع العمل
        Platform.runLater(() -> categoryCombo.requestFocus());
    }

    // --- التعديل الأول: جعل القائمة المنسدلة قابلة للبحث ---
    private void setupSearchableCategoryCombo() {
        ObservableList<ExpenseCategory> items = FXCollections.observableArrayList(categoryService.getAllCategories());
        FilteredList<ExpenseCategory> filteredItems = new FilteredList<>(items, p -> true);
        categoryCombo.setItems(filteredItems);

        categoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExpenseCategory cat) {
                return cat != null ? cat.getName() : "";
            }
            @Override
            public ExpenseCategory fromString(String string) {
                return items.stream().filter(item -> item.getName().equals(string)).findFirst().orElse(null);
            }
        });

        categoryCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            final TextField editor = categoryCombo.getEditor();
            final ExpenseCategory selected = categoryCombo.getSelectionModel().getSelectedItem();
            
            if (selected != null && selected.getName().equals(editor.getText())) {
                return;
            }
            
            filteredItems.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return item.getName().toLowerCase().contains(newValue.toLowerCase());
            });
            
            if (!filteredItems.isEmpty() && !categoryCombo.isShowing() && categoryCombo.getScene() != null && categoryCombo.getScene().getWindow() != null) {
                Platform.runLater(categoryCombo::show);
            }
        });
    }

    private void setupNumericValidation() {
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldVal);
            }
        });
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Expense) {
            // --- حالة التعديل ---
            this.currentExpense = (Expense) data;
            formTitleLabel.setText("تعديل المصروف رقم: " + currentExpense.getExpense_id());
            saveBtn.setText("تحديث البيانات");

            amountField.setText(currentExpense.getAmount().toString());
            descriptionArea.setText(currentExpense.getDescription());

            categoryCombo.getItems().stream()
                    .filter(cat -> cat.getCategory_id() == currentExpense.getCategory_id())
                    .findFirst()
                    .ifPresent(cat -> categoryCombo.setValue(cat));

            // توثيق الحالة القديمة للـ Audit Log
            oldDataJson = String.format("{\"amount\": %.2f, \"category_id\": %d, \"desc\": \"%s\"}", 
                    currentExpense.getAmount(), currentExpense.getCategory_id(), currentExpense.getDescription());
        } else {
            // --- حالة الإضافة ---
            this.currentExpense = null;
            formTitleLabel.setText("تسجيل مصروف جديد");
            saveBtn.setText("حفظ المصروف");
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        ExpenseCategory selectedCat = categoryCombo.getValue();
        String amountText = amountField.getText().trim();

        if (selectedCat == null || amountText.isEmpty()) {
            AlertManager.showError("خطأ", "يرجى تحديد الفئة والمبلغ.");
            return;
        }

        try {
            BigDecimal newAmount = new BigDecimal(amountText);
            if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertManager.showError("خطأ", "يجب أن يكون المبلغ أكبر من الصفر.");
                return;
            }

            // --- التعديل الثاني: رسالة التأكيد قبل الحفظ ---
            boolean isConfirmed = AlertManager.showConfirmation("تأكيد العملية", "هل أنت متأكد من حفظ هذا المصروف بقيمة " + amountText + " ل.س؟");
            if (!isConfirmed) {
                return; // إلغاء الحفظ إذا لم يوافق المستخدم
            }

            if (currentExpense == null) {
                // --- تنفيذ الإضافة ---
                Shift currentShift = SessionManager.getInstance().getCurrentShift();
                if (currentShift == null) {
                    AlertManager.showError("خطأ", "لا توجد وردية مفتوحة حالياً. لا يمكن تسجيل المصروف.");
                    return;
                }

                Expense newExpense = new Expense();
                newExpense.setAmount(newAmount);
                newExpense.setCategory_id(selectedCat.getCategory_id());
                newExpense.setDescription(descriptionArea.getText().trim());
                newExpense.setExpense_date(LocalDateTime.now());
                newExpense.setShift_id(currentShift.getShift_id());

                if (expenseService.createExpense(newExpense).isPresent()) {
                    AlertManager.showSuccess("تم بنجاح", "تم تسجيل المصروف الجديد وارتباطه بالوردية الحالية.");
                    closeWindow(event);
                }
            } else {
                // --- تنفيذ التعديل مع توثيق Audit Log ---
                currentExpense.setAmount(newAmount);
                currentExpense.setCategory_id(selectedCat.getCategory_id());
                currentExpense.setDescription(descriptionArea.getText().trim());

                if (expenseService.updateExpense(currentExpense)) {
                    
                    String newDataJson = String.format("{\"amount\": %.2f, \"category_id\": %d, \"desc\": \"%s\"}", 
                            currentExpense.getAmount(), currentExpense.getCategory_id(), currentExpense.getDescription());
                    
                    int currentUserId = SessionManager.getInstance().getCurrentUser().getUser_id();
                    AuditLog logEntry = new AuditLog(currentUserId, "UPDATE", "Expenses", oldDataJson, newDataJson, LocalDateTime.now());
                    auditLogDAO.create(logEntry);

                    AlertManager.showSuccess("تم بنجاح", "تم تحديث المصروف وتوثيق التعديل في سجلات المراقبة.");
                    closeWindow(event);
                }
            }
        } catch (NumberFormatException e) {
            AlertManager.showError("خطأ", "صيغة المبلغ غير صحيحة.");
        } catch (Exception e) {
            AlertManager.showError("خطأ تقني", "حدث خطأ أثناء حفظ المصروف.");
            e.printStackTrace();
        }
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