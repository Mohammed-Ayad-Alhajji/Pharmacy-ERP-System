// مسار الملف: src/main/java/com/pharmacy/dao/impl/security/RoleDAOImpl.java

package com.pharmacy.dao.impl.security;

import com.pharmacy.dao.interfaces.security.RoleDAO;
import com.pharmacy.models.security.Role;
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

public class RoleDAOImpl implements RoleDAO {

    @Override
    public Optional<Role> create(Role entity) {
        String sql = "INSERT INTO Roles (role_name, description) VALUES (?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getRole_name());
            
            if (entity.getDescription() != null) {
                pstmt.setString(2, entity.getDescription());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setRole_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج الدور. قد يكون الاسم مكرراً (UNIQUE Constraint): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Role> findById(Integer id) {
        String sql = "SELECT * FROM Roles WHERE role_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRole(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الدور: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Role> findAll() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM Roles";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                roles.add(mapRowToRole(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة الأدوار: " + e.getMessage());
        }
        return roles;
    }

    @Override
    public List<Role> findAll(int limit, int offset) {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM Roles LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(mapRowToRole(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب الأدوار المجدولة: " + e.getMessage());
        }
        return roles;
    }

    @Override
    public boolean update(Role entity) {
        String sql = "UPDATE Roles SET role_name = ?, description = ? WHERE role_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getRole_name());
            
            if (entity.getDescription() != null) {
                pstmt.setString(2, entity.getDescription());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            pstmt.setInt(3, entity.getRole_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات الدور. تأكد من عدم تكرار الاسم (UNIQUE Constraint): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Roles WHERE role_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("إجراء محظور: لا يمكن حذف هذا الدور لأنه مرتبط بمستخدمين في النظام");
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Roles WHERE role_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الدور: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Roles";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الأدوار: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Role> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Roles (role_name, description) VALUES (?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Role entity : entities) {
                    pstmt.setString(1, entity.getRole_name());
                    
                    if (entity.getDescription() != null) {
                        pstmt.setString(2, entity.getDescription());
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
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للأدوار. تم التراجع (Rollback). قد يوجد تكرار في الأسماء: " + e.getMessage());
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
    public Optional<Role> findByName(String name) {
        String sql = "SELECT * FROM Roles WHERE role_name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRole(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدور بالاسم: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Role mapRowToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setRole_id(rs.getInt("role_id"));
        role.setRole_name(rs.getString("role_name"));
        role.setDescription(rs.getString("description"));
        return role;
    }
    
    // مسار الملف: src/main/java/com/pharmacy/dao/impl/security/RolePermissionDAOImpl.java

    @Override
    public boolean updatePermissionsTransaction(int roleId, java.util.List<Integer> permissionIds) {
        String deleteSql = "DELETE FROM Role_Permissions WHERE role_id = ?";
        String insertSql = "INSERT INTO Role_Permissions (role_id, permission_id) VALUES (?, ?)";

        try (java.sql.Connection conn = com.pharmacy.utils.DBConnectionManager.getInstance().getConnection()) {
            // إيقاف الالتزام التلقائي لبدء المعاملة (Transaction)
            conn.setAutoCommit(false);

            try {
                // 1. تنفيذ استعلام الحذف
                try (java.sql.PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, roleId);
                    deleteStmt.executeUpdate();
                }

                // 2. تنفيذ استعلامات الإدراج مجمعة (Batch Processing)
                if (permissionIds != null && !permissionIds.isEmpty()) {
                    try (java.sql.PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        for (Integer permId : permissionIds) {
                            insertStmt.setInt(1, roleId);
                            insertStmt.setInt(2, permId);
                            insertStmt.addBatch();
                        }
                        insertStmt.executeBatch();
                    }
                }

                // 3. تأكيد المعاملة في حال نجاح جميع العمليات
                conn.commit();
                return true;

            } catch (java.sql.SQLException e) {
                // التراجع عن التغييرات في حال فشل أي جزء من العملية
                conn.rollback();
                System.err.println("[خطأ قاعدة بيانات] فشل تحديث الصلاحيات المجمعة. تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                // إعادة ضبط الالتزام التلقائي لتجنب التأثير على العمليات المستقبلية في Connection Pool (إن وجد)
                conn.setAutoCommit(true);
            }

        } catch (java.sql.SQLException e) {
            System.err.println("[خطأ اتصال] تعذر الاتصال بقاعدة البيانات لتنفيذ المعاملة: " + e.getMessage());
            return false;
        }
    }
}