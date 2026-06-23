// مسار الملف: src/main/java/com/pharmacy/dao/interfaces/security/RolePermissionDAO.java

package com.pharmacy.dao.interfaces.security;

import com.pharmacy.models.security.Permission;
import java.util.List;

/**
 * Data Access Object interface for Role_Permissions junction table.
 */
public interface RolePermissionDAO {
    
    boolean addPermissionToRole(int roleId, int permId);
    
    boolean removePermissionFromRole(int roleId, int permId);
    
    boolean removeAllPermissionsFromRole(int roleId);
    
    List<Permission> findPermissionsByRoleId(int roleId);
    
    boolean hasPermission(int roleId, String permName);

    /**
     * ينفذ عمليتي الحذف والإضافة داخل معاملة (Transaction) واحدة لضمان سلامة البيانات.
     */
    boolean updatePermissionsTransaction(int roleId, List<Integer> permissionIds);
}