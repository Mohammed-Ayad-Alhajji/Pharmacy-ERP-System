// المسار: src/main/java/com/pharmacy/controllers/layouts/MainLayoutController.java

package com.pharmacy.controllers.layouts;

import com.pharmacy.utils.gui.ViewManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    @FXML
    private BorderPane mainBorderPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // حقن الحاوية المركزية في مدير النوافذ لضمان إمكانية التبديل بين الشاشات مستقبلاً
        ViewManager.getInstance().setMainLayout(this.mainBorderPane);
    }
}