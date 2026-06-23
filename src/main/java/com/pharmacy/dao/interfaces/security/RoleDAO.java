// src/main/java/com/pharmacy/dao/interfaces/security/RoleDAO.java
package com.pharmacy.dao.interfaces.security;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.security.Role;
import java.util.Optional;

/**
 * Data Access Object interface for Role entity.
 */
public interface RoleDAO extends GenericDAO<Role, Integer> {
    
    Optional<Role> findByName(String name);
    boolean updatePermissionsTransaction(int roleId, java.util.List<Integer> permissionIds);
}