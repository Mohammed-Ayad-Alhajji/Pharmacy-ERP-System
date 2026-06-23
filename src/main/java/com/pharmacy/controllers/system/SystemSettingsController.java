package com.pharmacy.controllers.system;

import com.pharmacy.dao.impl.system.SystemSettingsDAOImpl;
import com.pharmacy.models.system.SystemSettings;
import com.pharmacy.services.impl.system.SystemSettingsServiceImpl;
import com.pharmacy.services.interfaces.system.SystemSettingsService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class SystemSettingsController {

    @FXML private TextField pharmacyNameField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField currencyField;
    @FXML private ImageView logoImageView;
    @FXML private Label logoPathLabel;

    private SystemSettingsService settingsService;
    private SystemSettings currentSettings;

    private String selectedLogoPath = "";

    @FXML
    public void initialize() {
        // تهيئة خدمة الإعدادات مع حقن الـ DAO الخاص بها
        settingsService = new SystemSettingsServiceImpl(new SystemSettingsDAOImpl());
        
        loadCurrentSettings();
    }

    private void loadCurrentSettings() {
        // جلب الإعدادات (الـ Service سيتكفل بإرجاع إعدادات افتراضية إذا كانت الداتابيز فارغة)
        currentSettings = settingsService.getSettings();

        // تعبئة الحقول
        pharmacyNameField.setText(currentSettings.getPharmacy_name());
        phoneField.setText(currentSettings.getPhone());
        addressField.setText(currentSettings.getAddress());
        currencyField.setText(currentSettings.getCurrency_symbol());

        // تحميل صورة الشعار إن وجدت
        selectedLogoPath = currentSettings.getLogo_path();
        updateLogoView();
    }

    @FXML
    private void handleChooseLogo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("اختيار شعار الصيدلية");
        
        // فلترة الملفات لتكون صوراً فقط
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // فتح نافذة الاختيار
        Stage stage = (Stage) pharmacyNameField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            selectedLogoPath = selectedFile.getAbsolutePath();
            updateLogoView();
        }
    }

    @FXML
    private void handleClearLogo(ActionEvent event) {
        selectedLogoPath = "";
        updateLogoView();
    }

    private void updateLogoView() {
        if (selectedLogoPath != null && !selectedLogoPath.trim().isEmpty()) {
            try {
                File file = new File(selectedLogoPath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    logoImageView.setImage(image);
                    logoPathLabel.setText(file.getName());
                    return;
                }
            } catch (Exception e) {
                System.err.println("فشل تحميل مسار الصورة: " + selectedLogoPath);
            }
        }
        
        // إذا لم يكن هناك مسار صالح، نعرض صورة فارغة
        logoImageView.setImage(null);
        logoPathLabel.setText("لم يتم تحديد صورة");
    }

    @FXML
    private void handleSaveSettings(ActionEvent event) {
        // 1. التحقق من صحة المدخلات
        String pharmacyName = pharmacyNameField.getText().trim();
        if (pharmacyName.isEmpty()) {
            AlertManager.showWarning("بيانات مطلوبة", "يجب إدخال اسم الصيدلية على الأقل.");
            return;
        }

        // 2. تحديث الكائن
        // نضمن أن setting_id = 1 دائماً كما تفرض قاعدة البيانات
        currentSettings.setSetting_id(1); 
        currentSettings.setPharmacy_name(pharmacyName);
        currentSettings.setPhone(phoneField.getText().trim());
        currentSettings.setAddress(addressField.getText().trim());
        
        String currency = currencyField.getText().trim();
        currentSettings.setCurrency_symbol(currency.isEmpty() ? "ل.س" : currency);
        
        currentSettings.setLogo_path(selectedLogoPath);

        // 3. إرسال البيانات للحفظ
        boolean isSaved = settingsService.saveSettings(currentSettings);

        if (isSaved) {
            AlertManager.showSuccess("نجاح", "تم حفظ إعدادات النظام بنجاح.");
            // ملاحظة: يمكنك هنا مستقبلاً استدعاء دالة لتحديث الشعار في الـ Sidebar إذا أردت!
        } else {
            AlertManager.showError("خطأ", "حدث خطأ أثناء حفظ الإعدادات في قاعدة البيانات.");
        }
    }
}