// مسار الملف: src/main/java/com/pharmacy/services/impl/system/AuditLogServiceImpl.java

package com.pharmacy.services.impl.system;

import com.pharmacy.dao.interfaces.system.AuditLogDAO;
import com.pharmacy.models.system.AuditLog;
import com.pharmacy.services.interfaces.system.AuditLogService;
import com.pharmacy.security.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogDAO auditLogDAO;

    public AuditLogServiceImpl(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    @Override
    public void logAction(String actionType, String tableAffected, String oldData, String newData) {
        try {
            int userId;
            try {
                userId = SessionManager.getInstance().getCurrentUserId();
            } catch (IllegalStateException e) {
                userId = 0; // System Action
            }

            AuditLog log = new AuditLog();
            // استخدام Integer لحقل user_id يتماشى مع الموديل لتجنب أخطاء القيم الخالية (Null)
            log.setUser_id(userId == 0 ? null : userId); 
            log.setAction_type(actionType);
            log.setTable_affected(tableAffected);
            
            // هنا التعديل الجوهري: نمرر البيانات القديمة والجديدة
            log.setOld_data(oldData);
            log.setNew_data(newData);
            
            log.setAction_timestamp(LocalDateTime.now());

            auditLogDAO.create(log);
        } catch (Exception e) {
            System.err.println("[خطأ تنفيذي]: فشل في تسجيل حركة المراقبة في قاعدة البيانات. التفاصيل: " + e.getMessage());
        }
    }

    @Override
    public List<AuditLog> getAllLogs(int limit, int offset) {
        // افتراض استخدام اسم findAll في طبقة الـ DAO كمعيار قياسي
        return auditLogDAO.findAll(limit, offset);
    }

    @Override
    public List<AuditLog> getLogsByUser(int userId, int limit, int offset) {
        return auditLogDAO.findByUserId(userId, limit, offset);
    }

    @Override
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end, int limit, int offset) {
        return auditLogDAO.findByDateRange(start, end, limit, offset);
    }

    @Override
    public List<AuditLog> getLogsByAction(String actionType, int limit, int offset) {
        return auditLogDAO.findByActionType(actionType, limit, offset);
    }

    @Override
    public long getTotalLogsCount() {
        return auditLogDAO.count();
    }
}