// مسار الملف: src/main/java/com/pharmacy/services/interfaces/security/RoleService.java

package com.pharmacy.services.interfaces.security;

import com.pharmacy.models.security.Permission;
import com.pharmacy.models.security.Role;
import com.pharmacy.utils.exceptions.SecurityException;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    
    List<Role> getAllRoles();
    
    Optional<Role> getRoleById(int roleId);
    
    boolean createRole(Role role) throws SecurityException;
    
    boolean updateRole(Role role) throws SecurityException;
    
    boolean deleteRole(int roleId) throws SecurityException;
    
    List<Permission> getAllPermissions();
    
    List<Permission> getPermissionsForRole(int roleId);
    
    boolean updateRolePermissions(int roleId, List<Integer> permissionIds) throws SecurityException;
    
    
    
    boolean hasPermission(Role role, String permissionName);
}