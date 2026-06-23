package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.CategoryDAOImpl;
import com.pharmacy.models.inventory.Category;
import com.pharmacy.services.impl.inventory.CategoryServiceImpl;
import com.pharmacy.services.interfaces.inventory.CategoryService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.DataTransferable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class CategoryFormController implements DataTransferable {

    @FXML private Label formTitleLabel;
    @FXML private TextField nameField;
    @FXML private TextArea descArea;
    @FXML private Button saveBtn;

    private CategoryService categoryService;
    private Category currentCategory; // null = إضافة، غير ذلك = تعديل

    @FXML
    public void initialize() {
        categoryService = new CategoryServiceImpl(new CategoryDAOImpl());
    }

    // هنا يكمن الذكاء: استقبال البيانات من الجدول
    @Override
    public void receiveData(Object data) {
        if (data instanceof Category) {
            // حالة التعديل
            this.currentCategory = (Category) data;
            formTitleLabel.setText("تعديل الصنف: " + currentCategory.getName());
            saveBtn.setText("تحديث البيانات");
            
            nameField.setText(currentCategory.getName());
            descArea.setText(currentCategory.getDescription());
        } else {
            // حالة الإضافة
            this.currentCategory = null;
            formTitleLabel.setText("إضافة صنف دوائي جديد");
            saveBtn.setText("حفظ الصنف");
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        String desc = descArea.getText().trim();

        if (name.isEmpty()) {
            AlertManager.showError("خطأ", "حقل اسم الصنف إلزامي.");
            return;
        }

        try {
            if (currentCategory == null) {
                // تنفيذ الإضافة
                Category newCat = new Category();
                newCat.setName(name);
                newCat.setDescription(desc);

                Optional<Category> saved = categoryService.createCategory(newCat);
                if (categoryService.updateCategory(currentCategory)) {
                    AlertManager.showSuccess("تم التحديث", "تم تعديل بيانات الصنف بنجاح.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "لم يتم تحديث البيانات، يرجى المحاولة مرة أخرى.");
                }
            } else {
                // تنفيذ التعديل
                currentCategory.setName(name);
                currentCategory.setDescription(desc);

                if (categoryService.updateCategory(currentCategory)) {
                    AlertManager.showSuccess("تم التحديث", "تم تعديل بيانات الصنف بنجاح.");
                    closeWindow(event);
                }
            }
        } catch (IllegalArgumentException e) {
            AlertManager.showError("تنبيه", e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ تقني", "حدث خطأ غير متوقع.");
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