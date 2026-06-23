package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.CategoryDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.models.inventory.Category;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.services.impl.inventory.CategoryServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.interfaces.inventory.CategoryService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.DataTransferable;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.Optional;

public class MedicineFormController implements DataTransferable {

    @FXML private Label formTitleLabel;
    @FXML private TextField barcodeField;
    @FXML private TextField brandNameField;
    @FXML private TextField genericNameField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField dosageFormField;
    @FXML private TextField conversionFactorField;
    @FXML private TextField boxPriceField;
    @FXML private TextField unitPriceField;
    @FXML private TextField minStockField;
    @FXML private TextField shelfLocationField;
    @FXML private CheckBox prescriptionCheck;
    @FXML private CheckBox activeCheck;
    @FXML private Button saveBtn;

    private MedicineService medicineService;
    private CategoryService categoryService;
    private Medicine currentMedicine; // لتحديد ما إذا كنا في وضع الإضافة أم التعديل

    @FXML
    public void initialize() {
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        categoryService = new CategoryServiceImpl(new CategoryDAOImpl());

        loadCategories();
        setupNumericValidation();
    }

    private void loadCategories() {
        categoryCombo.setItems(FXCollections.observableArrayList(categoryService.getAllCategories()));
        categoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category cat) {
                return cat != null ? cat.getName() : "";
            }
            @Override
            public Category fromString(String string) { return null; }
        });
    }

    // إجبار المستخدم على إدخال أرقام فقط في الحقول الحسابية
    private void setupNumericValidation() {
        boxPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) boxPriceField.setText(oldVal);
        });
        unitPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) unitPriceField.setText(oldVal);
        });
        conversionFactorField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) conversionFactorField.setText(oldVal);
        });
        minStockField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) minStockField.setText(oldVal);
        });
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Medicine) {
            this.currentMedicine = (Medicine) data;
            fillFormForEdit();
        } else {
            // القيم الافتراضية لحالة الإضافة
            conversionFactorField.setText("1");
            minStockField.setText("5");
        }
    }

    private void fillFormForEdit() {
        formTitleLabel.setText("تعديل بيانات الدواء: " + currentMedicine.getBrand_name());
        saveBtn.setText("تحديث البيانات");

        // استخدام (Ternary Operator) لمنع الـ Null من الدخول لحقول النص
        barcodeField.setText(currentMedicine.getBarcode() == null ? "" : currentMedicine.getBarcode());
        brandNameField.setText(currentMedicine.getBrand_name() == null ? "" : currentMedicine.getBrand_name());
        genericNameField.setText(currentMedicine.getGeneric_name() == null ? "" : currentMedicine.getGeneric_name());
        dosageFormField.setText(currentMedicine.getDosage_form() == null ? "" : currentMedicine.getDosage_form());
        conversionFactorField.setText(String.valueOf(currentMedicine.getConversion_factor()));
        boxPriceField.setText(currentMedicine.getCurrent_box_sell_price() == null ? "0" : currentMedicine.getCurrent_box_sell_price().toString());
        unitPriceField.setText(currentMedicine.getCurrent_unit_sell_price() == null ? "0" : currentMedicine.getCurrent_unit_sell_price().toString());
        minStockField.setText(String.valueOf(currentMedicine.getMin_stock_level()));
        shelfLocationField.setText(currentMedicine.getShelf_location() == null ? "" : currentMedicine.getShelf_location());
        
        prescriptionCheck.setSelected(currentMedicine.getPrescription_required() == 1);
        activeCheck.setSelected(currentMedicine.getIs_active() == 1);

        // تحديد التصنيف
        categoryCombo.getItems().stream()
            .filter(cat -> cat.getCategory_id() == currentMedicine.getCategory_id())
            .findFirst()
            .ifPresent(cat -> categoryCombo.setValue(cat));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!validateInputs()) return;

        try {
            boolean isNew = (currentMedicine == null);
            Medicine medicine = isNew ? new Medicine() : currentMedicine;

            // حماية إضافية: إذا كان الحقل فارغاً أو Null نضع نصاً فارغاً قبل الحفظ
            medicine.setBarcode(barcodeField.getText() == null ? "" : barcodeField.getText().trim());
            medicine.setBrand_name(brandNameField.getText() == null ? "" : brandNameField.getText().trim());
            medicine.setGeneric_name(genericNameField.getText() == null ? "" : genericNameField.getText().trim());
            medicine.setCategory_id(categoryCombo.getValue().getCategory_id());
            medicine.setDosage_form(dosageFormField.getText() == null ? "" : dosageFormField.getText().trim());
            medicine.setConversion_factor(Integer.parseInt(conversionFactorField.getText().trim()));
            medicine.setCurrent_box_sell_price(new BigDecimal(boxPriceField.getText().trim()));
            medicine.setCurrent_unit_sell_price(new BigDecimal(unitPriceField.getText().trim()));
            medicine.setMin_stock_level(Integer.parseInt(minStockField.getText().trim()));
            medicine.setShelf_location(shelfLocationField.getText() == null ? "" : shelfLocationField.getText().trim());
            medicine.setPrescription_required(prescriptionCheck.isSelected() ? 1 : 0);
            medicine.setIs_active(activeCheck.isSelected() ? 1 : 0);

            if (isNew) {
                Optional<Medicine> saved = medicineService.createMedicine(medicine);
                if (saved.isPresent()) {
                    AlertManager.showSuccess("نجاح", "تمت إضافة الدواء بنجاح.");
                    closeWindow(event);
                }
            } else {
                if (medicineService.updateMedicine(medicine)) {
                    AlertManager.showSuccess("نجاح", "تم تحديث بيانات الدواء.");
                    closeWindow(event);
                } else {
                    AlertManager.showError("خطأ", "فشل تحديث البيانات. يرجى المحاولة مرة أخرى أو التأكد من عدم تكرار الباركود.");
                }
            }
        } catch (IllegalArgumentException e) {
            AlertManager.showError("خطأ في البيانات", e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ تقني", "حدث خطأ غير متوقع: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        // اللمسة 2: تم إزالة التحقق من فراغ الباركود بناءً على طلبك
        if (brandNameField.getText().trim().isEmpty() || genericNameField.getText().trim().isEmpty()) {
            AlertManager.showError("خطأ", "الاسم التجاري والعلمي مطلوبان.");
            return false;
        }
        if (categoryCombo.getValue() == null) {
            AlertManager.showError("خطأ", "يرجى اختيار تصنيف الدواء.");
            return false;
        }
        if (boxPriceField.getText().trim().isEmpty() || unitPriceField.getText().trim().isEmpty() || conversionFactorField.getText().trim().isEmpty()) {
            AlertManager.showError("خطأ", "تأكد من إدخال الأسعار ومعامل التحويل (العبوة تحتوي على).");
            return false;
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