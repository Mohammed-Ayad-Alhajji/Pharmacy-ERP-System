// مسار الملف: src/main/java/com/pharmacy/services/interfaces/system/SystemSettingsService.java

package com.pharmacy.services.interfaces.system;

import com.pharmacy.models.system.SystemSettings;

public interface SystemSettingsService {
    
    SystemSettings getSettings();
    
    boolean saveSettings(SystemSettings settings);
}