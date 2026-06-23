// src/main/java/com/pharmacy/dao/interfaces/system/SystemSettingsDAO.java
package com.pharmacy.dao.interfaces.system;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.system.SystemSettings;
import java.util.Optional;

/**
 * Data Access Object for System Settings.
 * ARCHITECTURAL CONSTRAINT FOR IMPLEMENTATION:
 * - delete(ID id): MUST throw UnsupportedOperationException (Table restricted to setting_id = 1).
 * - create(T entity): MUST be implemented as an UPSERT (Update if exists, Insert if empty).
 */
public interface SystemSettingsDAO extends GenericDAO<SystemSettings, Integer> {
    
    Optional<SystemSettings> getCurrentSettings();
}