// src/main/java/com/pharmacy/dao/interfaces/security/PermissionDAO.java
package com.pharmacy.dao.interfaces.security;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.security.Permission;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Permission entity.
 */
public interface PermissionDAO extends GenericDAO<Permission, Integer> {
    
    Optional<Permission> findByName(String name);
    
    List<Permission> findByModule(String module);
}