package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.DrugAlternativeDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.services.impl.inventory.DrugAlternativeServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.interfaces.inventory.DrugAlternativeService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

public class MedicineAlternativesController {

    // === عناصر النصف الأيمن ===
    @FXML private TextField searchMainField;
    @FXML private TableView<Medicine> mainTable;
    @FXML private TableColumn<Medicine, String> mainBrandCol, mainGenericCol;

    // === عناصر النصف الأيسر ===
    @FXML private VBox leftPanel;
    @FXML private Label lblSelectedMedicine;
    @FXML private TableView<Medicine> altTable;
    @FXML private TableColumn<Medicine, String> altBrandCol, altGenericCol;
    @FXML private TableColumn<Medicine, Void> altActionCol;
    @FXML private ComboBox<Medicine> searchAltCombo;

    private MedicineService medicineService;
    private DrugAlternativeService altService;

    private ObservableList<Medicine> masterMedicinesList = FXCollections.observableArrayList();
    private FilteredList<Medicine> filteredMainList;
    private FilteredList<Medicine> filteredAltComboList;
    
    private Medicine selectedMainMedicine = null;

    @FXML
    public void initialize() {
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        altService = new DrugAlternativeServiceImpl(new DrugAlternativeDAOImpl());

        setupMainTable();
        setupAltTable();
        setupSearchableComboBox();

        loadAllMedicines();
    }

    private void loadAllMedicines() {
        List<Medicine> allMeds = medicineService.getAllMedicines(); // استبدلها بالدالة التي تجلب الأدوية النشطة إذا أردت
        masterMedicinesList.setAll(allMeds);

        // إعداد الفلترة للجدول الأيمن
        filteredMainList = new FilteredList<>(masterMedicinesList, p -> true);
        mainTable.setItems(filteredMainList);

        searchMainField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredMainList.setPredicate(med -> {
                if (newVal == null || newVal.trim().isEmpty()) return true;
                String filter = newVal.toLowerCase();
                return (med.getBrand_name() != null && med.getBrand_name().toLowerCase().contains(filter)) ||
                       (med.getGeneric_name() != null && med.getGeneric_name().toLowerCase().contains(filter)) ||
                       (med.getBarcode() != null && med.getBarcode().contains(filter));
            });
        });
    }

    private void setupMainTable() {
        mainBrandCol.setCellValueFactory(new PropertyValueFactory<>("brand_name"));
        mainGenericCol.setCellValueFactory(new PropertyValueFactory<>("generic_name"));

        // مستمع لاختيار دواء من الجدول الأيمن
        mainTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedMainMedicine = newSelection;
                lblSelectedMedicine.setText(selectedMainMedicine.getBrand_name());
                leftPanel.setDisable(false);
                refreshAltTable();
                
                // إعادة تهيئة الكومبوبوكس لكي لا يعرض الدواء نفسه كبديل
                setupSearchableComboBox(); 
            } else {
                leftPanel.setDisable(true);
                selectedMainMedicine = null;
                lblSelectedMedicine.setText("[لم يتم اختيار دواء]");
                altTable.getItems().clear();
            }
        });
    }

    private void setupAltTable() {
        altBrandCol.setCellValueFactory(new PropertyValueFactory<>("brand_name"));
        altGenericCol.setCellValueFactory(new PropertyValueFactory<>("generic_name"));

        altActionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("❌");
            {
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-cursor: hand; -fx-font-size: 14px;");
                deleteBtn.setOnAction(e -> {
                    Medicine altMed = getTableView().getItems().get(getIndex());
                    handleRemoveAlternative(altMed);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void refreshAltTable() {
        if (selectedMainMedicine == null) return;
        List<Medicine> alternatives = altService.getAlternatives(selectedMainMedicine.getMed_id());
        altTable.setItems(FXCollections.observableArrayList(alternatives));
    }

    private void setupSearchableComboBox() {
        searchAltCombo.getItems().clear();
        filteredAltComboList = new FilteredList<>(masterMedicinesList, p -> {
            // لا نعرض الدواء المحدد حالياً في قائمة البدائل المتاحة
            if (selectedMainMedicine != null && p.getMed_id() == selectedMainMedicine.getMed_id()) {
                return false;
            }
            return true;
        });
        searchAltCombo.setItems(filteredAltComboList);

        searchAltCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Medicine m) { return m != null ? m.getBrand_name() : ""; }
            @Override public Medicine fromString(String s) {
                return masterMedicinesList.stream().filter(m -> m.getBrand_name().equals(s)).findFirst().orElse(null);
            }
        });

        searchAltCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            final TextField editor = searchAltCombo.getEditor();
            final Medicine selected = searchAltCombo.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getBrand_name().equals(editor.getText())) return;

            filteredAltComboList.setPredicate(med -> {
                if (selectedMainMedicine != null && med.getMed_id() == selectedMainMedicine.getMed_id()) return false;
                if (newVal == null || newVal.trim().isEmpty()) return true;
                
                String filter = newVal.toLowerCase();
                return (med.getBrand_name() != null && med.getBrand_name().toLowerCase().contains(filter)) ||
                       (med.getGeneric_name() != null && med.getGeneric_name().toLowerCase().contains(filter));
            });

            if (!filteredAltComboList.isEmpty() && !searchAltCombo.isShowing() && searchAltCombo.getScene() != null) {
                Platform.runLater(searchAltCombo::show);
            }
        });
    }

    @FXML
    private void handleAddAlternative(ActionEvent event) {
        if (selectedMainMedicine == null) return;

        Medicine altMedicine = searchAltCombo.getSelectionModel().getSelectedItem();
        
        if (altMedicine == null || searchAltCombo.getEditor().getText().isEmpty()) {
            AlertManager.showWarning("تنبيه", "يرجى اختيار الدواء البديل من القائمة أولاً.");
            return;
        }

        try {
            boolean success = altService.addAlternative(selectedMainMedicine.getMed_id(), altMedicine.getMed_id());
            if (success) {
                // تصفير الكومبوبوكس وتحديث الجدول
                searchAltCombo.getSelectionModel().clearSelection();
                searchAltCombo.getEditor().clear();
                refreshAltTable();
            } else {
                AlertManager.showError("خطأ", "فشلت عملية الربط. ربما الدواء مربوط مسبقاً.");
            }
        } catch (IllegalArgumentException e) {
            AlertManager.showError("تنبيه", e.getMessage());
        }
    }

    private void handleRemoveAlternative(Medicine altMedicine) {
        if (selectedMainMedicine == null || altMedicine == null) return;

        boolean confirmed = AlertManager.showConfirmation("تأكيد", "هل أنت متأكد من إلغاء الربط بين الدوائين؟");
        if (confirmed) {
            boolean success = altService.removeAlternative(selectedMainMedicine.getMed_id(), altMedicine.getMed_id());
            if (success) {
                refreshAltTable();
            } else {
                AlertManager.showError("خطأ", "فشلت عملية إلغاء الربط.");
            }
        }
    }
}