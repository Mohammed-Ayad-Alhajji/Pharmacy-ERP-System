// مسار الملف: src/main/java/com/pharmacy/services/impl/security/UserServiceImpl.java

package com.pharmacy.services.impl.security;

import com.pharmacy.dao.interfaces.security.UserDAO;
import com.pharmacy.models.security.User;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.utils.exceptions.AuthenticationException;
import com.pharmacy.utils.security.PasswordUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
    
    @Override
    public User login(String username, String plainPassword) throws AuthenticationException {
        Optional<User> optUser = userDAO.findByUsername(username);
        
        if (!optUser.isPresent() || !PasswordUtil.verifyPassword(plainPassword, optUser.get().getPassword_hash())) {
            throw new AuthenticationException("بيانات الدخول غير صحيحة");
        }
        
        User user = optUser.get();
        if (!user.isActive()) {
            throw new AuthenticationException("هذا الحساب موقوف");
        }
        
        return user;
    }

    @Override
    public boolean registerUser(User user, String plainPassword) {
        if (isUsernameTaken(user.getUsername())) {
            return false;
        }
        user.setPassword_hash(PasswordUtil.hashPassword(plainPassword));
        return userDAO.create(user).isPresent();
    }

    @Override
    public boolean updateUser(User user, String newPlainPassword) {
        if (newPlainPassword != null && !newPlainPassword.trim().isEmpty()) {
            user.setPassword_hash(PasswordUtil.hashPassword(newPlainPassword));
        } else {
            Optional<User> existingUser = userDAO.findById(user.getUser_id());
            existingUser.ifPresent(u -> user.setPassword_hash(u.getPassword_hash()));
        }
        return userDAO.update(user);
    }

    @Override
    public boolean deactivateUser(int userId) {
        Optional<User> optUser = userDAO.findById(userId);
        if (optUser.isPresent()) {
            User user = optUser.get();
            user.setActive(false);
            return userDAO.update(user);
        }
        return false;
    }

    @Override
    public boolean activateUser(int userId) {
        Optional<User> optUser = userDAO.findById(userId);
        if (optUser.isPresent()) {
            User user = optUser.get();
            user.setActive(true);
            return userDAO.update(user);
        }
        return false;
    }

    @Override
    public boolean deleteUser(int userId) {
        return userDAO.delete(userId);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return userDAO.findByUsername(username).isPresent();
    }

    @Override
    public List<User> getUsers(boolean includeInactive, int limit, int offset) {
        if (includeInactive) {
            return userDAO.findAll(limit, offset);
        }
        return userDAO.findActiveUsers().stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> searchUsers(String keyword, boolean includeInactive, int limit, int offset) {
        List<User> results = userDAO.searchByName(keyword);
        
        if (!includeInactive) {
            results = results.stream()
                    .filter(User::isActive)
                    .collect(Collectors.toList());
        }
        
        return results.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long getUsersCount(boolean includeInactive) {
        if (includeInactive) {
            return userDAO.count();
        }
        return userDAO.findActiveUsers().size();
    }
    
    @Override
    public Optional<User> getUserById(int userId) {
        // بافتراض أن لديك كائن userDAO محقون في هذا الكلاس
        return userDAO.getUserById(userId); 
    }
}