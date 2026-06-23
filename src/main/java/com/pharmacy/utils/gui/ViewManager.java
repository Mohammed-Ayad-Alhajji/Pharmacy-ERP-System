// المسار: src/main/java/com/pharmacy/utils/gui/ViewManager.java

package com.pharmacy.utils.gui;

import com.pharmacy.security.SessionManager;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * مدير الواجهات (ViewManager)
 * مسؤول عن التنقل الموحد بين الشاشات وإدارة النوافذ المنبثقة وحماية المسارات المالية.
 */
public class ViewManager {

    private static volatile ViewManager instance;
    private BorderPane mainLayout;

    private ViewManager() {}

    public static ViewManager getInstance() {
        if (instance == null) {
            synchronized (ViewManager.class) {
                if (instance == null) {
                    instance = new ViewManager();
                }
            }
        }
        return instance;
    }

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

    public void toggleSidebar() {
        if (mainLayout != null && mainLayout.getLeft() != null) {
            Region sidebar = (Region) mainLayout.getLeft();
            double originalWidth = 260; // تم تعديلها لتطابق عرض الـ Sidebar.fxml لدينا
            boolean isOpening = !sidebar.isVisible();

            sidebar.setMinWidth(0); 

            if (sidebar instanceof javafx.scene.layout.Pane) {
                ((javafx.scene.layout.Pane) sidebar).getChildren().forEach(node -> {
                    if (node instanceof Region) {
                        ((Region) node).setMinWidth(originalWidth);
                    }
                });
            }

            javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle();
            clipRect.setHeight(2000); 
            clipRect.widthProperty().bind(sidebar.prefWidthProperty());
            sidebar.setClip(clipRect);

            Timeline timeline = new Timeline();

            if (isOpening) {
                sidebar.setVisible(true);
                sidebar.setManaged(true);
                KeyFrame kf = new KeyFrame(Duration.millis(300),
                    new KeyValue(sidebar.prefWidthProperty(), originalWidth, Interpolator.LINEAR)
                );
                timeline.getKeyFrames().add(kf);
            } else {
                KeyFrame kf = new KeyFrame(Duration.millis(300),
                    new KeyValue(sidebar.prefWidthProperty(), 0, Interpolator.LINEAR)
                );
                timeline.setOnFinished(e -> {
                    sidebar.setVisible(false);
                    sidebar.setManaged(false);
                });
                timeline.getKeyFrames().add(kf);
            }
            timeline.play();
        }
    }

    /**
     * الانتقال العادي بين الشاشات وتحميلها في منتصف الحاوية الرئيسية.
     */
    public void switchScene(String fxmlPath) {
        if (mainLayout == null) {
            System.err.println("[خطأ معمارية]: لا يمكن التبديل، لم يتم حقن الحاوية الرئيسية (MainLayout).");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            mainLayout.setCenter(root);
        } catch (IOException e) {
            System.err.println("[فشل واجهة]: تعذر تحميل الملف: " + fxmlPath);
            e.printStackTrace();
            AlertManager.showError("خطأ في النظام", "لم يتم العثور على الواجهة المطلوبة أو فشل تحميلها.");
        }
    }

    /**
     * الحارس المالي (Financial Route Guard):
     * يحمي الواجهات المالية ويمنع فتحها إذا لم تكن هناك وردية عمل (Shift) مفتوحة.
     */
    public void switchFinancialScene(String fxmlPath) {
        if (SessionManager.getInstance().getCurrentShift() != null) {
            switchScene(fxmlPath);
        } else {
            System.err.println("[حارس المسار]: محاولة وصول مالي مرفوضة، لا توجد وردية مفتوحة.");
            // استخدام AlertManager بدلاً من شاشة كاملة
            AlertManager.showWarning("إجراء مرفوض", "لا يمكنك الدخول إلى شاشات المبيعات أو الصندوق لعدم وجود وردية مفتوحة. يرجى فتح وردية جديدة أولاً.");
        }
    }

    /**
     * عرض نافذة منبثقة (Modal) مقفلة التفاعل الخارجي حتى تُغلق.
     */
    public void showModal(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.setTitle(title);
            modalStage.setScene(new Scene(root));
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.showAndWait();
        } catch (IOException e) {
            System.err.println("[فشل نافذة منبثقة]: تعذر تحميل الملف: " + fxmlPath);
            e.printStackTrace();
        }
    }
    
    /**
     * استبدال جذر النافذة الحالية بالكامل (للتنقل من Login إلى MainLayout).
     */
    public void changeRootWindow(String fxmlPath, Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            // 1. إخفاء النافذة القديمة (شاشة الدخول) لحظياً حتى لا يرى المستخدم القفزة
            currentStage.hide();
            
            // 2. إعداد المشهد الجديد
            currentStage.setScene(scene);
            currentStage.setTitle("نظام إدارة الصيدلية - الشاشة الرئيسية");
            currentStage.setResizable(true); 
            currentStage.setMinWidth(1024);
            currentStage.setMinHeight(768);
            
            // 3. تطبيق التكبير (Maximized) والنافذة مخفية (وهذا يمنع الرجفة)
            currentStage.setMaximized(true); 

            // 4. إظهار النافذة وهي بكامل حجمها مباشرة
            currentStage.show(); 

        } catch (IOException e) {
            System.err.println("[فشل جذري]: تعذر تبديل نافذة الجذر: " + fxmlPath);
            e.printStackTrace();
        }
    }
    /**
     * فتح نافذة منبثقة (Modal) مع تمرير بيانات إلى الكنترولر الخاص بها
     */
    public void showModalWithData(String fxmlPath, String title, Object data) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            
            // السحر الهندسي: إرسال البيانات للكنترولر قبل عرض النافذة
            Object controller = loader.getController();
            if (controller instanceof DataTransferable) {
                ((DataTransferable) controller).receiveData(data);
            }
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(title);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
            
        } catch (Exception e) {
            System.err.println("خطأ في فتح النافذة المنبثقة مع البيانات: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * العودة لشاشة تسجيل الدخول وإغلاق النافذة الرئيسية.
     */
    public void openLoginWindow(Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/views/auth/LoginView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            Stage loginStage = new Stage();
            loginStage.setTitle("نظام إدارة الصيدلية - تسجيل الدخول");
            loginStage.setScene(scene);
            loginStage.setResizable(false); // شاشة الدخول يفضل أن تكون ثابتة الحجم
            loginStage.centerOnScreen();
            
            loginStage.show();
            currentStage.close(); // إغلاق الشاشة الرئيسية الكبيرة
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}