// المسار: src/main/java/com/pharmacy/utils/gui/AlertManager.java

package com.pharmacy.utils.gui;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import java.util.Optional;
import javafx.scene.control.ButtonBar;

/**
 * كلاس مركزي لإدارة وعرض الرسائل التنبيهية للمستخدم.
 * يضمن توحيد شكل الرسائل واتجاهها (RTL) في كافة أنحاء النظام.
 */
public class AlertManager {

    /**
     * عرض رسالة نجاح (أخضر)
     */
    public static void showSuccess(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    /**
     * عرض رسالة خطأ (أحمر)
     */
    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    /**
     * عرض رسالة تحذير (أصفر)
     */
    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    public static boolean showConfirmation(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // 1. إزالة الأيقونة الافتراضية المزعجة
        alert.setGraphic(null);

        // 2. إعداد الأزرار
        javafx.scene.control.ButtonType yesButton = new javafx.scene.control.ButtonType("نعم", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType noButton = new javafx.scene.control.ButtonType("إلغاء", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yesButton, noButton);

        // 3. التنسيق الأساسي للنافذة 
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        // تم زيادة المساحة العلوية (30px) لكي لا يصطدم النص المرفوع بسقف النافذة
        dialogPane.setStyle("-fx-background-color: #ffffff; -fx-padding: 30px 20px 20px 20px;");

        // 4. تنسيق النص الداخلي (رفع النص للأعلى وإضافة Margin عن الأزرار)
        javafx.scene.Node contentLabel = dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-font-family: 'Segoe UI'; " +
                                  "-fx-font-size: 16px; " +
                                  "-fx-font-weight: bold; " +
                                  "-fx-text-fill: #2c3e50; " +
                                  "-fx-alignment: leaft; " +
                                  "-fx-translate-y: -10px; " +   /* هذا السطر يرفع النص للأعلى */
                                  "-fx-padding: 0 0 30px 0;");  /* هذا السطر يضع مسافة (Margin) أسفل النص ليدفع الأزرار */
        }

        // 5. الوصول للأزرار وتنسيقها
        javafx.scene.control.Button yesBtn = (javafx.scene.control.Button) dialogPane.lookupButton(yesButton);
        javafx.scene.control.Button noBtn = (javafx.scene.control.Button) dialogPane.lookupButton(noButton);

        if (yesBtn != null) {
            String yesNormal = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20 8 20; -fx-background-radius: 4; -fx-cursor: hand;";
            String yesHover = "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20 8 20; -fx-background-radius: 4; -fx-cursor: hand;";
            yesBtn.setStyle(yesNormal);
            yesBtn.setOnMouseEntered(e -> yesBtn.setStyle(yesHover));
            yesBtn.setOnMouseExited(e -> yesBtn.setStyle(yesNormal));
        }

        if (noBtn != null) {
            String noNormal = "-fx-background-color: #f1f2f6; -fx-text-fill: #2f3640; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: #dcdde1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 7 20 7 20; -fx-cursor: hand;";
            String noHover = "-fx-background-color: #dfe4ea; -fx-text-fill: #2f3640; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: #dcdde1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 7 20 7 20; -fx-cursor: hand;";
            noBtn.setStyle(noNormal);
            noBtn.setOnMouseEntered(e -> noBtn.setStyle(noHover));
            noBtn.setOnMouseExited(e -> noBtn.setStyle(noNormal));
        }

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }

    /**
     * الدالة الأساسية الداخلية التي تبني النوافذ
     */
    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // ضبط الاتجاه ليكون من اليمين لليسار (RTL)
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        alert.showAndWait();
    }
}