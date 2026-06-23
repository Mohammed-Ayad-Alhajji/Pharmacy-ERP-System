// المسار: src/main/java/com/pharmacy/main/MainApp.java

package com.pharmacy.main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. تحديد مسار أول شاشة يجب أن تظهر للمستخدم (شاشة تسجيل الدخول)
        DatabaseSeeder.seed();
        String loginFxmlPath = "/com/pharmacy/views/auth/LoginView.fxml";
       
        // 2. تحميل ملف الواجهة
        FXMLLoader loader = new FXMLLoader(getClass().getResource(loginFxmlPath));
        Parent root = loader.load();
        
        // 3. وضع الواجهة داخل مشهد (Scene)
        Scene scene = new Scene(root);
        
        // 4. إعدادات النافذة الرئيسية (النافذة الأم)
        primaryStage.setTitle("نظام إدارة الصيدلية - تسجيل الدخول");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true); // منع تغيير حجم شاشة الدخول للحفاظ على جمالية التصميم
        primaryStage.centerOnScreen();    // توسيط النافذة في الشاشة
        primaryStage.setMaximized(true);
        // 5. إظهار النافذة
        primaryStage.show();
    }

    public static void main(String[] args) {
        // هذه الدالة هي التي تقوم بتشغيل دورة حياة JavaFX
        launch(args);
    }
}