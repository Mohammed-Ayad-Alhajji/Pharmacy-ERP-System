// مسار الملف: src/main/java/com/pharmacy/dao/impl/system/SystemSettingsDAOImpl.java

package com.pharmacy.dao.impl.system;

import com.pharmacy.dao.interfaces.system.SystemSettingsDAO;
import com.pharmacy.models.system.SystemSettings;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SystemSettingsDAOImpl implements SystemSettingsDAO {

    @Override
    public Optional<SystemSettings> create(SystemSettings entity) {
        String sql = "INSERT INTO System_Settings (setting_id, pharmacy_name, logo_path, address, phone, currency_symbol) " +
                     "VALUES (1, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(setting_id) DO UPDATE SET " +
                     "pharmacy_name = excluded.pharmacy_name, " +
                     "logo_path = excluded.logo_path, " +
                     "address = excluded.address, " +
                     "phone = excluded.phone, " +
                     "currency_symbol = excluded.currency_symbol";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getPharmacy_name());
            
            if (entity.getLogo_path() != null) {
                pstmt.setString(2, entity.getLogo_path());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getAddress() != null) {
                pstmt.setString(3, entity.getAddress());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            if (entity.getPhone() != null) {
                pstmt.setString(4, entity.getPhone());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            
            if (entity.getCurrency_symbol() != null) {
                pstmt.setString(5, entity.getCurrency_symbol());
            } else {
                pstmt.setString(5, "ل.س"); // القيمة الافتراضية كما في الجدول
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                entity.setSetting_id(1); // فرض المعرف الدائم للـ Singleton
                return Optional.of(entity);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج/تحديث إعدادات النظام عبر دالة UPSERT: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean update(SystemSettings entity) {
        // بالاعتماد على خوارزمية UPSERT في دالة create، نقوم بتوجيه التحديث إليها مباشرة
        return create(entity).isPresent();
    }

    @Override
    public boolean delete(Integer id) {
        throw new UnsupportedOperationException("إجراء محظور: لا يمكن حذف إعدادات النظام الأساسية (Singleton Constraint)");
    }

    @Override
    public Optional<SystemSettings> findById(Integer id) {
        String sql = "SELECT * FROM System_Settings WHERE setting_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSettings(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات إعدادات النظام: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<SystemSettings> getCurrentSettings() {
        // توجيه الدالة لجلب الصف الوحيد بناءً على القيد الثابت (setting_id = 1)
        return findById(1);
    }

    @Override
    public List<SystemSettings> findAll() {
        List<SystemSettings> settingsList = new ArrayList<>();
        getCurrentSettings().ifPresent(settingsList::add);
        return settingsList;
    }

    @Override
    public List<SystemSettings> findAll(int limit, int offset) {
        List<SystemSettings> settingsList = new ArrayList<>();
        String sql = "SELECT * FROM System_Settings LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    settingsList.add(mapRowToSettings(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب إعدادات النظام المجدولة: " + e.getMessage());
        }
        return settingsList;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM System_Settings WHERE setting_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود إعدادات النظام: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM System_Settings";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب سجلات النظام: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<SystemSettings> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        boolean isSuccess = true;
        for (SystemSettings entity : entities) {
            if (!update(entity)) {
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    private SystemSettings mapRowToSettings(ResultSet rs) throws SQLException {
        SystemSettings settings = new SystemSettings();
        settings.setSetting_id(rs.getInt("setting_id"));
        settings.setPharmacy_name(rs.getString("pharmacy_name"));
        settings.setLogo_path(rs.getString("logo_path"));
        settings.setAddress(rs.getString("address"));
        settings.setPhone(rs.getString("phone"));
        settings.setCurrency_symbol(rs.getString("currency_symbol"));
        return settings;
    }
}