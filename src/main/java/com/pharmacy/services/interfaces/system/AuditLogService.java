// مسار الملف: src/main/java/com/pharmacy/services/interfaces/system/AuditLogService.java

package com.pharmacy.services.interfaces.system;

import com.pharmacy.models.system.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {

    void logAction(String actionType, String tableAffected, String oldData, String newData);

    List<AuditLog> getAllLogs(int limit, int offset);

    List<AuditLog> getLogsByUser(int userId, int limit, int offset);

    List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end, int limit, int offset);

    List<AuditLog> getLogsByAction(String actionType, int limit, int offset);

    long getTotalLogsCount();
}