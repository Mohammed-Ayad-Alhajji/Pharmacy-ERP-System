// مسار الملف: src/main/java/com/pharmacy/services/impl/system/SystemSettingsServiceImpl.java

package com.pharmacy.services.impl.system;

import com.pharmacy.dao.interfaces.system.SystemSettingsDAO;
import com.pharmacy.models.system.SystemSettings;
import com.pharmacy.services.interfaces.system.SystemSettingsService;

import java.util.Optional;

public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SystemSettingsDAO systemSettingsDAO;

    // حقن الاعتمادية (Dependency Injection)
    public SystemSettingsServiceImpl(SystemSettingsDAO systemSettingsDAO) {
        this.systemSettingsDAO = systemSettingsDAO;
    }

    @Override
    public SystemSettings getSettings() {
        Optional<SystemSettings> optSettings = systemSettingsDAO.getCurrentSettings();
        
        if (optSettings.isPresent()) {
            return optSettings.get();
        }

        // إنشاء كائن افتراضي كإجراء وقائي (Fail-Safe) لمنع NullPointerException في واجهات JavaFX
        SystemSettings defaultSettings = new SystemSettings();
        defaultSettings.setPharmacy_name("صيدليتي");
        defaultSettings.setCurrency_symbol("ل.س");
        
        // استبدال الحقول الوهمية بالحقول الحقيقية الموجودة في قاعدة البيانات
        defaultSettings.setAddress("عنوان الصيدلية - غير محدد");
        defaultSettings.setPhone("رقم الهاتف - غير محدد");
        defaultSettings.setLogo_path(""); // مسار فارغ كافتراضي
        
        return defaultSettings;
    }

    @Override
    public boolean saveSettings(SystemSettings settings) {
        if (settings == null) {
            return false;
        }
        
        // الاعتماد على السلوك المعماري لطبقة הـ DAO التي تستخدم استعلام UPSERT
        Optional<SystemSettings> savedSettings = systemSettingsDAO.create(settings);
        return savedSettings.isPresent();
    }
}