// مسار الملف: src/main/java/com/pharmacy/dao/impl/security/PermissionDAOImpl.java

package com.pharmacy.dao.impl.security;

import com.pharmacy.dao.interfaces.security.PermissionDAO;
import com.pharmacy.models.security.Permission;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PermissionDAOImpl implements PermissionDAO {

    @Override
    public Optional<Permission> create(Permission entity) {
        String sql = "INSERT INTO Permissions (perm_name, module) VALUES (?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getPerm_name());
            
            if (entity.getModule() != null) {
                pstmt.setString(2, entity.getModule());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setPerm_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج الصلاحية. قد يكون الاسم مكرراً (UNIQUE Constraint): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Permission> findById(Integer id) {
        String sql = "SELECT * FROM Permissions WHERE perm_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPermission(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الصلاحية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Permission> findAll() {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT * FROM Permissions";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                permissions.add(mapRowToPermission(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة الصلاحيات: " + e.getMessage());
        }
        return permissions;
    }

    @Override
    public List<Permission> findAll(int limit, int offset) {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT * FROM Permissions LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapRowToPermission(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب الصلاحيات المجدولة: " + e.getMessage());
        }
        return permissions;
    }

    @Override
    public boolean update(Permission entity) {
        String sql = "UPDATE Permissions SET perm_name = ?, module = ? WHERE perm_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getPerm_name());
            
            if (entity.getModule() != null) {
                pstmt.setString(2, entity.getModule());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            pstmt.setInt(3, entity.getPerm_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات الصلاحية. تأكد من عدم تكرار الاسم (UNIQUE Constraint): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Permissions WHERE perm_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("إجراء محظور: لا يمكن حذف هذه الصلاحية لأنها مخصصة لأدوار في النظام");
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Permissions WHERE perm_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الصلاحية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Permissions";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الصلاحيات: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Permission> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Permissions (perm_name, module) VALUES (?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Permission entity : entities) {
                    pstmt.setString(1, entity.getPerm_name());
                    
                    if (entity.getModule() != null) {
                        pstmt.setString(2, entity.getModule());
                    } else {
                        pstmt.setNull(2, Types.VARCHAR);
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للصلاحيات. تم التراجع (Rollback). قد يوجد تكرار في الأسماء: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Permission> findByName(String name) {
        String sql = "SELECT * FROM Permissions WHERE perm_name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPermission(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الصلاحية بالاسم: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Permission> findByModule(String module) {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT * FROM Permissions WHERE module = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, module);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapRowToPermission(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الصلاحيات حسب الوحدة: " + e.getMessage());
        }
        return permissions;
    }

    private Permission mapRowToPermission(ResultSet rs) throws SQLException {
        Permission permission = new Permission();
        permission.setPerm_id(rs.getInt("perm_id"));
        permission.setPerm_name(rs.getString("perm_name"));
        permission.setModule(rs.getString("module"));
        return permission;
    }
}