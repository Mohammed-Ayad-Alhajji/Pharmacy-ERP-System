// مسار الملف: src/main/java/com/pharmacy/services/interfaces/UserService.java

package com.pharmacy.services.interfaces.security;

import com.pharmacy.models.security.User;
import com.pharmacy.utils.exceptions.AuthenticationException;

import java.util.List;
import java.util.Optional;

public interface UserService {
    
    User login(String username, String plainPassword) throws AuthenticationException;
    
    boolean registerUser(User user, String plainPassword);
    
    boolean updateUser(User user, String newPlainPassword);
    
    boolean deactivateUser(int userId);
    
    boolean activateUser(int userId);
    
    boolean deleteUser(int userId);
    
    boolean isUsernameTaken(String username);
    
    List<User> getUsers(boolean includeInactive, int limit, int offset);
    
    List<User> searchUsers(String keyword, boolean includeInactive, int limit, int offset);
    
    long getUsersCount(boolean includeInactive);
    // إضافة الدالة الجديدة لجلب مستخدم عن طريق الـ ID
    Optional<User> getUserById(int userId);
}