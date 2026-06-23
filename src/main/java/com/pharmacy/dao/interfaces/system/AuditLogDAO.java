// src/main/java/com/pharmacy/dao/interfaces/system/AuditLogDAO.java
package com.pharmacy.dao.interfaces.system;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.system.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object interface for AuditLog entity.
 * Updated with pagination methods to prevent OutOfMemory errors on large logs.
 */
public interface AuditLogDAO extends GenericDAO<AuditLog, Integer> {
    
    // الدوال القديمة
    List<AuditLog> findByUserId(int userId);
    
    List<AuditLog> findByActionType(String actionType);
    
    List<AuditLog> findByTableAffected(String tableName);
    
    List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // الدوال الجديدة المخصصة للـ Pagination (Micro-task 6.1)
    List<AuditLog> findByUserId(int userId, int limit, int offset);
    
    List<AuditLog> findByActionType(String actionType, int limit, int offset);
    
    List<AuditLog> findByTableAffected(String tableName, int limit, int offset);
    
    List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int limit, int offset);
}