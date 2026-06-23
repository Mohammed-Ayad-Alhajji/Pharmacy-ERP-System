// المسار: src/main/java/com/pharmacy/security/SessionManager.java
package com.pharmacy.security;

import com.pharmacy.models.security.User;
import com.pharmacy.models.security.Shift; // استيراد كلاس الوردية
import java.util.HashSet;
import java.util.Set;

public class SessionManager {

    private static volatile SessionManager instance;
    private User currentUser;
    private Shift currentShift; // متغير لتخزين الوردية المفتوحة حالياً
    private Set<String> userPermissions = new HashSet<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void startSession(User user, Set<String> permissions) {
        this.currentUser = user;
        this.userPermissions = permissions != null ? permissions : new HashSet<>();
    }

    public void endSession() {
        this.currentUser = null;
        this.currentShift = null; // مسح بيانات الوردية عند تسجيل الخروج
        if (this.userPermissions != null) {
            this.userPermissions.clear();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public int getCurrentRoleId() {
        if (currentUser != null) {
            return currentUser.getRole_id();
        }
        return -1;
    }

    public boolean hasPermission(String permissionName) {
        return userPermissions != null && userPermissions.contains(permissionName);
    }

    // --- الدوال الجديدة الخاصة بالوردية (لحل خطأ ViewManager) ---

    public void setCurrentShift(Shift shift) {
        this.currentShift = shift;
    }

    public Shift getCurrentShift() {
        return currentShift;
    }
    
    public void clearShift() {
        this.currentShift = null;
    }
    public int getCurrentUserId() {
        if (currentUser != null) {
            return currentUser.getUser_id();
        }
        return -1; // أو يمكنك رمي Exception هنا
    }
    /**
     * تسجيل خروج المستخدم الحالي وتفريغ بيانات الجلسة من الذاكرة
     * ملاحظة: هذا الإجراء لا يغلق الوردية في قاعدة البيانات
     */
    public void logout() {
        this.currentUser = null;
        
        if (this.userPermissions != null) {
            this.userPermissions.clear();
        }
        
        // نقوم بتفريغ الوردية من الذاكرة الحالية فقط
        // لكي يتمكن المستخدم القادم من تحميل ورديته الخاصة به عند تسجيل الدخول
        this.currentShift = null; 
        
        System.out.println("تم تسجيل الخروج وتفريغ الجلسة بنجاح.");
    }
}