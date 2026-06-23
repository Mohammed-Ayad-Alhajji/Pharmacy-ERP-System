// مسار الملف: src/main/java/com/pharmacy/dao/impl/security/RolePermissionDAOImpl.java

package com.pharmacy.dao.impl.security;

import com.pharmacy.dao.interfaces.security.RolePermissionDAO;
import com.pharmacy.models.security.Permission;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RolePermissionDAOImpl implements RolePermissionDAO {

    @Override
    public boolean addPermissionToRole(int roleId, int permId) {
        String sql = "INSERT INTO Role_Permissions (role_id, perm_id) VALUES (?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roleId);
            pstmt.setInt(2, permId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean removePermissionFromRole(int roleId, int permId) {
        String sql = "DELETE FROM Role_Permissions WHERE role_id = ? AND perm_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roleId);
            pstmt.setInt(2, permId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إزالة الصلاحية المحددة من الدور: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeAllPermissionsFromRole(int roleId) {
        String sql = "DELETE FROM Role_Permissions WHERE role_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roleId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تفريغ الصلاحيات المرتبطة بالدور: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Permission> findPermissionsByRoleId(int roleId) {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT p.perm_id, p.perm_name, p.module FROM Permissions p INNER JOIN Role_Permissions rp ON p.perm_id = rp.perm_id WHERE rp.role_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Permission permission = new Permission();
                    permission.setPerm_id(rs.getInt("perm_id"));
                    permission.setPerm_name(rs.getString("perm_name"));
                    permission.setModule(rs.getString("module"));
                    permissions.add(permission);
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الصلاحيات المرتبطة بالدور: " + e.getMessage());
        }
        return permissions;
    }

    @Override
    public boolean hasPermission(int roleId, String permName) {
        String sql = "SELECT 1 FROM Role_Permissions rp INNER JOIN Permissions p ON rp.perm_id = p.perm_id WHERE rp.role_id = ? AND p.perm_name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roleId);
            pstmt.setString(2, permName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الصلاحية للدور: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updatePermissionsTransaction(int roleId, List<Integer> permissionIds) {
        String deleteSql = "DELETE FROM Role_Permissions WHERE role_id = ?";
        String insertSql = "INSERT INTO Role_Permissions (role_id, perm_id) VALUES (?, ?)";

        // فتح اتصال واحد لكامل المعاملة
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            // إيقاف الالتزام التلقائي لضمان تنفيذ العمليات ككتلة واحدة (ACID)
            conn.setAutoCommit(false);

            try {
                // 1. تفريغ الصلاحيات القديمة
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, roleId);
                    deleteStmt.executeUpdate();
                }

                // 2. إدراج الصلاحيات الجديدة عبر Batch Processing للأداء العالي
                if (permissionIds != null && !permissionIds.isEmpty()) {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        for (Integer permId : permissionIds) {
                            insertStmt.setInt(1, roleId);
                            insertStmt.setInt(2, permId);
                            insertStmt.addBatch();
                        }
                        insertStmt.executeBatch();
                    }
                }

                // 3. تأكيد التغييرات إذا نجحت كل الخطوات السابقة
                conn.commit();
                return true;

            } catch (SQLException e) {
                // التراجع عن كل التغييرات فور حدوث أي خطأ
                conn.rollback();
                System.err.println("[خطأ قاعدة بيانات] فشل تحديث الصلاحيات المجمعة. تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                // إعادة حالة الاتصال لطبيعتها قبل إغلاقه (مهم جداً إذا كان هناك Connection Pool)
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[خطأ اتصال] تعذر الاتصال بقاعدة البيانات لتنفيذ المعاملة: " + e.getMessage());
            return false;
        }
    }
}