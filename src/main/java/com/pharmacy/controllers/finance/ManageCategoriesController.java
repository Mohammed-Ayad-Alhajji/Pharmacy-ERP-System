package com.pharmacy.controllers.finance;

import com.pharmacy.dao.impl.finance.ExpenseCategoryDAOImpl;
import com.pharmacy.models.finance.ExpenseCategory;
import com.pharmacy.services.impl.finance.ExpenseCategoryServiceImpl;
import com.pharmacy.services.interfaces.finance.ExpenseCategoryService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Optional;

public class ManageCategoriesController {

    @FXML private TextField newCategoryField;
    @FXML private TableView<ExpenseCategory> categoriesTable;
    @FXML private TableColumn<ExpenseCategory, Integer> idCol;
    @FXML private TableColumn<ExpenseCategory, String> nameCol;
    @FXML private TableColumn<ExpenseCategory, Void> actionCol;

    private ExpenseCategoryService categoryService;
    private ObservableList<ExpenseCategory> categoriesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        categoryService = new ExpenseCategoryServiceImpl(new ExpenseCategoryDAOImpl());

        idCol.setCellValueFactory(new PropertyValueFactory<>("category_id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        setupActionColumn();
        loadCategories();
    }

    private void loadCategories() {
        categoriesList.clear();
        categoriesList.addAll(categoryService.getAllCategories());
        categoriesTable.setItems(categoriesList);
    }

    @FXML
    private void handleAddCategory(ActionEvent event) {
        String name = newCategoryField.getText().trim();
        if (name.isEmpty()) {
            AlertManager.showError("خطأ", "يرجى إدخال اسم الفئة أولاً.");
            return;
        }

        try {
            ExpenseCategory newCat = new ExpenseCategory(name);
            Optional<ExpenseCategory> saved = categoryService.createCategory(newCat);

            if (saved.isPresent()) {
                newCategoryField.clear();
                loadCategories();
                AlertManager.showSuccess("نجاح", "تمت إضافة الفئة بنجاح.");
            } else {
                AlertManager.showError("خطأ", "فشل الحفظ في قاعدة البيانات.");
            }
        } catch (IllegalArgumentException e) {
            // التقاط الخطأ القادم من السيرفس (الاسم موجود مسبقاً) وعرضه
            AlertManager.showError("تنبيه", e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ تقني", "حدث خطأ غير متوقع.");
            e.printStackTrace();
        }
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("حذف 🗑️");
            {
                deleteBtn.getStyleClass().add("button-danger");
                deleteBtn.setOnAction(e -> {
                    ExpenseCategory cat = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(cat);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });
    }

    private void handleDeleteCategory(ExpenseCategory cat) {
        boolean confirm = AlertManager.showConfirmation("تأكيد", "هل أنت متأكد من حذف فئة (" + cat.getName() + ")؟");
        if (confirm) {
            // ملاحظة: الحذف سيفشل تلقائياً بواسطة SQLite (RESTRICT) إذا كانت الفئة مستخدمة في أي مصروف قديم!
            if (categoryService.deleteCategory(cat.getCategory_id())) {
                loadCategories();
            } else {
                AlertManager.showError("لا يمكن الحذف", "هذه الفئة مستخدمة بالفعل في مصروفات مسجلة مسبقاً. الحذف سيؤدي إلى تلف السجلات المالية.");
            }
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}