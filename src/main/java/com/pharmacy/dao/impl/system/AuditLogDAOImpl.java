// مسار الملف: src/main/java/com/pharmacy/dao/impl/system/AuditLogDAOImpl.java

package com.pharmacy.dao.impl.system;

import com.pharmacy.dao.interfaces.system.AuditLogDAO;
import com.pharmacy.models.system.AuditLog;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditLogDAOImpl implements AuditLogDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<AuditLog> create(AuditLog entity) {
        String sql = "INSERT INTO Audit_Logs (user_id, action_type, table_affected, old_data, new_data, action_timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (entity.getUser_id() == null || entity.getUser_id() == 0) {
                pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, entity.getUser_id());
            }

            pstmt.setString(2, entity.getAction_type());
            pstmt.setString(3, entity.getTable_affected());

            if (entity.getOld_data() != null) {
                pstmt.setString(4, entity.getOld_data());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }

            if (entity.getNew_data() != null) {
                pstmt.setString(5, entity.getNew_data());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }

            if (entity.getAction_timestamp() != null) {
                pstmt.setString(6, entity.getAction_timestamp().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setLog_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج سجل المراقبة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<AuditLog> findById(Integer id) {
        String sql = "SELECT * FROM Audit_Logs WHERE log_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات سجل المراقبة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<AuditLog> findAll() {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                logs.add(mapRowToAuditLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة سجلات المراقبة: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findAll(int limit, int offset) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب سجلات المراقبة المجدولة: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public boolean update(AuditLog entity) {
        System.err.println("اختراق أمني: يُمنع منعاً باتاً تعديل سجلات المراقبة (Audit Logs)!");
        return false;
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("اختراق أمني: يُمنع منعاً باتاً تعديل سجلات المراقبة (Audit Logs)!");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Audit_Logs WHERE log_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود سجل المراقبة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Audit_Logs";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي سجلات المراقبة: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<AuditLog> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }

        String sql = "INSERT INTO Audit_Logs (user_id, action_type, table_affected, old_data, new_data, action_timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (AuditLog entity : entities) {
                    if (entity.getUser_id() == null || entity.getUser_id() == 0) {
                        pstmt.setNull(1, Types.INTEGER);
                    } else {
                        pstmt.setInt(1, entity.getUser_id());
                    }

                    pstmt.setString(2, entity.getAction_type());
                    pstmt.setString(3, entity.getTable_affected());

                    if (entity.getOld_data() != null) {
                        pstmt.setString(4, entity.getOld_data());
                    } else {
                        pstmt.setNull(4, Types.VARCHAR);
                    }

                    if (entity.getNew_data() != null) {
                        pstmt.setString(5, entity.getNew_data());
                    } else {
                        pstmt.setNull(5, Types.VARCHAR);
                    }

                    if (entity.getAction_timestamp() != null) {
                        pstmt.setString(6, entity.getAction_timestamp().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }

                    pstmt.addBatch();
                }

                pstmt.executeBatch();
                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لسجلات المراقبة. تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private AuditLog mapRowToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLog_id(rs.getInt("log_id"));

        int userId = rs.getInt("user_id");
        if (rs.wasNull()) {
            log.setUser_id(null);
        } else {
            log.setUser_id(userId);
        }

        log.setAction_type(rs.getString("action_type"));
        log.setTable_affected(rs.getString("table_affected"));
        log.setOld_data(rs.getString("old_data"));
        log.setNew_data(rs.getString("new_data"));

        String timestampStr = rs.getString("action_timestamp");
        if (timestampStr != null && !timestampStr.trim().isEmpty()) {
            log.setAction_timestamp(LocalDateTime.parse(timestampStr, DATE_TIME_FORMATTER));
        }

        return log;
    }

    @Override
    public List<AuditLog> findByUserId(int userId) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE user_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجلات المراقبة للمستخدم: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findByUserId(int userId, int limit, int offset) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE user_id = ? LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام المجدول عن سجلات المراقبة للمستخدم: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findByActionType(String actionType) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE action_type = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, actionType);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجلات المراقبة حسب نوع الإجراء: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findByActionType(String actionType, int limit, int offset) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE action_type = ? LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, actionType);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام المجدول عن سجلات المراقبة حسب نوع الإجراء: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findByTableAffected(String tableName) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE table_affected = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tableName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجلات المراقبة حسب الجدول المتأثر: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findByTableAffected(String tableName, int limit, int offset) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE table_affected = ? LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tableName);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام المجدول عن سجلات المراقبة حسب الجدول المتأثر: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE action_timestamp BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startDate.format(DATE_TIME_FORMATTER));
            pstmt.setString(2, endDate.format(DATE_TIME_FORMATTER));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجلات المراقبة ضمن النطاق الزمني: " + e.getMessage());
        }
        return logs;
    }

    @Override
    public List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int limit, int offset) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Audit_Logs WHERE action_timestamp BETWEEN ? AND ? LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startDate.format(DATE_TIME_FORMATTER));
            pstmt.setString(2, endDate.format(DATE_TIME_FORMATTER));
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام المجدول عن سجلات المراقبة ضمن النطاق الزمني: " + e.getMessage());
        }
        return logs;
    }
}