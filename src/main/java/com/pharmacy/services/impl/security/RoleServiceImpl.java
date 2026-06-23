// مسار الملف: src/main/java/com/pharmacy/services/impl/security/RoleServiceImpl.java

package com.pharmacy.services.impl.security;

import com.pharmacy.dao.interfaces.security.PermissionDAO;
import com.pharmacy.dao.interfaces.security.RoleDAO;
import com.pharmacy.dao.interfaces.security.RolePermissionDAO;
import com.pharmacy.models.security.Permission;
import com.pharmacy.models.security.Role;
import com.pharmacy.services.interfaces.security.RoleService;
import com.pharmacy.utils.exceptions.SecurityException;

import java.util.List;
import java.util.Optional;

public class RoleServiceImpl implements RoleService {

    private final RoleDAO roleDAO;
    private final PermissionDAO permissionDAO;
    private final RolePermissionDAO rolePermissionDAO;

    public RoleServiceImpl(RoleDAO roleDAO, PermissionDAO permissionDAO, RolePermissionDAO rolePermissionDAO) {
        this.roleDAO = roleDAO;
        this.permissionDAO = permissionDAO;
        this.rolePermissionDAO = rolePermissionDAO;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleDAO.findAll();
    }

    @Override
    public Optional<Role> getRoleById(int roleId) {
        return roleDAO.findById(roleId);
    }

    @Override
    public boolean createRole(Role role) throws SecurityException {
        Optional<Role> created = roleDAO.create(role);
        if (!created.isPresent()) {
            throw new SecurityException("فشل في حفظ بيانات الدور.");
        }
        return true;
    }

    @Override
    public boolean updateRole(Role role) throws SecurityException {
        if (!roleDAO.update(role)) {
            throw new SecurityException("فشل في تحديث بيانات الدور.");
        }
        return true;
    }

    @Override
    public boolean deleteRole(int roleId) throws SecurityException {
        try {
            boolean isDeleted = roleDAO.delete(roleId);
            if (!isDeleted) {
                throw new SecurityException("لا يمكن حذف هذا الدور لوجود مستخدمين أو صلاحيات مرتبطة به.");
            }
            return true;
        } catch (Exception e) {
            throw new SecurityException("لا يمكن حذف هذا الدور لوجود مستخدمين مرتبطين به. قم بتغيير أدوارهم أولاً.");
        }
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionDAO.findAll();
    }

    @Override
    public List<Permission> getPermissionsForRole(int roleId) {
        return rolePermissionDAO.findPermissionsByRoleId(roleId);
    }

    @Override
    public boolean updateRolePermissions(int roleId, java.util.List<Integer> permissionIds) throws com.pharmacy.utils.exceptions.SecurityException {
        // الاعتماد الكلي على طبقة DAO لإدارة المعاملة لضمان الفصل الصارم للمسؤوليات (Separation of Concerns)
        boolean isUpdated = rolePermissionDAO.updatePermissionsTransaction(roleId, permissionIds);
        
        if (!isUpdated) {
            throw new com.pharmacy.utils.exceptions.SecurityException("فشل في تحديث صلاحيات الدور. تم التراجع عن التغييرات للحفاظ على سلامة البيانات.");
        }
        return true;
    }

    @Override
    public boolean hasPermission(Role role, String permissionName) {
        if (role == null || permissionName == null || permissionName.trim().isEmpty()) {
            return false;
        }
        
        // التحقق الذكي بناءً على خاصية الكائن بدلاً من الأسماء الثابتة (Open/Closed Principle)
        if (role.isSuperuser()) {
            return true;
        }
        
        return rolePermissionDAO.hasPermission(role.getRole_id(), permissionName);
    }
}