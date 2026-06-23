// src/main/java/com/pharmacy/dao/interfaces/security/UserDAO.java
package com.pharmacy.dao.interfaces.security;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.security.User;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for User entity.
 * Updated to support administrative search functionality.
 */
public interface UserDAO extends GenericDAO<User, Integer> {
    
    Optional<User> findByUsername(String username);
    
    List<User> findByRoleId(int roleId);
    
    List<User> findActiveUsers();

    List<User> searchByName(String keyword);
    Optional<User> getUserById(int userId);
}