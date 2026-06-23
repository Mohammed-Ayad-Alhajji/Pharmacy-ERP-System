// المسار المؤقت: src/main/java/com/pharmacy/main/PasswordResetter.java

package com.pharmacy.main;

import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.models.security.User;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.interfaces.security.UserService;
import java.util.List;

public class PasswordResetter {

    public static void main(String[] args) {
        System.out.println("⏳ بدء عملية تحديث تشفير كلمات المرور للمستخدمين الحاليين...");

        UserService userService = new UserServiceImpl(new UserDAOImpl());
        
        // جلب جميع المستخدمين من قاعدة البيانات
        List<User> allUsers = userService.getUsers(true, 100, 0);

        if(allUsers.isEmpty()) {
            System.out.println("⚠️ لا يوجد مستخدمين في قاعدة البيانات!");
            return;
        }

        // تحديث كلمة المرور لجميع المستخدمين لتصبح "123456"
        for (User user : allUsers) {
            // الدالة updateUser ستقوم داخلياً باستدعاء PasswordUtil لتشفير الكلمة الجديدة
            boolean success = userService.updateUser(user, "123");
            if (success) {
                System.out.println("✅ تم تحديث التشفير للمستخدم: " + user.getUsername() + " | كلمة المرور الجديدة: 123456");
            } else {
                System.out.println("❌ فشل تحديث المستخدم: " + user.getUsername());
            }
        }

        System.out.println("\n🎉 تم الانتهاء بنجاح! يمكنك الآن تسجيل الدخول باستخدام حساباتك القديمة بكلمة المرور: 123456");
    }
}