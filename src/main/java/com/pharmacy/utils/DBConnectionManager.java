// مسار الملف: src/main/java/com/pharmacy/utils/DBConnectionManager.java

package com.pharmacy.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionManager {

    // استخدام volatile ضروري لضمان تحديث المتغير فوراً في الذاكرة لجميع الـ Threads
    private static volatile DBConnectionManager instance;
    private static final String DB_URL = "jdbc:sqlite:pharmacy.db?foreign_keys=on";

    private DBConnectionManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("[خطأ فادح]: تعذر العثور على مشغل SQLite JDBC.", e);
        }
    }

    // تطبيق Double-Checked Locking لأداء مثالي مع الـ Threads
    public static DBConnectionManager getInstance() {
        if (instance == null) {
            synchronized (DBConnectionManager.class) {
                if (instance == null) {
                    instance = new DBConnectionManager();
                }
            }
        }
        return instance;
    }

    // الدالة ترمي الاستثناء بدلاً من إرجاع null لمنع انهيار النظام بشكل غير متوقع في طبقة DAO
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}