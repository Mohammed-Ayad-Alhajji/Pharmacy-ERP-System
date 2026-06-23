// المسار: src/main/java/com/pharmacy/main/DatabaseSeeder.java

package com.pharmacy.main;

import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.models.security.User;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.interfaces.security.UserService;

public class DatabaseSeeder {

    // تحويل الدالة إلى دالة مخصصة للتغذية بدلاً من دالة التشغيل main
    public static void seed() {
        System.out.println("🔄 جاري التحقق من الحسابات الافتراضية للنظام...");
        
        UserService userService = new UserServiceImpl(new UserDAOImpl());

        // 1. حساب المدير (Admin) - تفترض البنية أن المعامل الأول هو المعرف أو دور المستخدم
        createUserIfNotExist(userService, new User(1, "admin", "", "المدير العام", true), "admin123", "المدير العام");

        // 2. حساب الصيدلي (Pharmacist)
        createUserIfNotExist(userService, new User(2, "ph", "", "صيدلي المبيعات", true), "ph123", "صيدلي المبيعات");

        // 3. حساب أمين المستودع (Inventory Manager)
        createUserIfNotExist(userService, new User(3, "inv", "", "أمين المستودع", true), "inv123", "أمين المستودع");

        // 4. حساب المحاسب (Accountant)
        createUserIfNotExist(userService, new User(4, "acc", "", "محاسب النظام", true), "acc123", "محاسب النظام");

        System.out.println("🎉 انتهت عملية فحص وتغذية قاعدة البيانات.\n");
    }

    // دالة مساعدة لمنع تكرار كود الطباعة والتحقق (DRY Principle)
    private static void createUserIfNotExist(UserService userService, User user, String password, String roleName) {
        try {
            boolean isCreated = userService.registerUser(user, password);
            String status = isCreated ? "تم الإنشاء بنجاح [OK]" : "موجود مسبقاً";
            System.out.printf("- حساب %-20s: %s%n", roleName, status);
        } catch (Exception e) {
            System.err.printf("❌ خطأ أثناء إعداد حساب %s: %s%n", roleName, e.getMessage());
        }
    }
}