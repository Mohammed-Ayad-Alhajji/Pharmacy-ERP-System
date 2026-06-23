package com.pharmacy.controllers.inventory;

import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.InventoryAdjustment;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.gui.DataTransferable;

import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.InventoryAdjustmentService;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.InventoryAdjustmentServiceImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.InventoryAdjustmentDAOImpl;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;

public class AddAdjustmentController implements DataTransferable {

    @FXML private ComboBox<Medicine> medicineCombo;
    @FXML private ComboBox<Batch> batchCombo;
    @FXML private TextField systemQtyField;
    @FXML private TextField actualQtyField;
    @FXML private TextField differenceField;
    @FXML private TextArea notesArea;

    private MedicineService medicineService;
    private BatchService batchService;
    private InventoryAdjustmentService adjustmentService;

    @FXML
    public void initialize() {
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        batchService = new BatchServiceImpl(new BatchDAOImpl());
        adjustmentService = new InventoryAdjustmentServiceImpl(new InventoryAdjustmentDAOImpl());

        setupSearchableMedicineCombo();
        setupQuantityCalculations();
        
        // التركيز التلقائي على حقل البحث عن دواء لتسريع العمل
        Platform.runLater(() -> medicineCombo.requestFocus());
    }

    @Override
    public void receiveData(Object data) {
        // هذه النافذة للإضافة فقط
    }

    // ==========================================
    // 1. إعداد البحث الذكي للأدوية والطبخات
    // ==========================================
    private void setupSearchableMedicineCombo() {
        // --- إعداد قائمة الأدوية ---
        ObservableList<Medicine> medItems = FXCollections.observableArrayList(medicineService.getActiveMedicines());
        FilteredList<Medicine> filteredMeds = new FilteredList<>(medItems, p -> true);
        medicineCombo.setItems(filteredMeds);

        medicineCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Medicine m) { return m != null ? m.getBrand_name() : ""; }
            @Override public Medicine fromString(String string) {
                return medItems.stream().filter(item -> item.getBrand_name().equals(string)).findFirst().orElse(null);
            }
        });

        // فلترة الأدوية بالاسم والباركود
        medicineCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            final TextField editor = medicineCombo.getEditor();
            final Medicine selected = medicineCombo.getSelectionModel().getSelectedItem();

            if (selected != null && selected.getBrand_name().equals(editor.getText())) return;

            filteredMeds.setPredicate(item -> {
                if (newValue == null || newValue.trim().isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                boolean matchName = item.getBrand_name().toLowerCase().contains(lowerCaseFilter);
                boolean matchBarcode = item.getBarcode() != null && item.getBarcode().contains(lowerCaseFilter);
                return matchName || matchBarcode;
            });

            if (!filteredMeds.isEmpty() && !medicineCombo.isShowing() && medicineCombo.getScene() != null) {
                Platform.runLater(medicineCombo::show);
            }
        });

        // --- عند اختيار الدواء، إعداد قائمة الطبخات (Batches) للبحث ---
        medicineCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<Batch> batches = batchService.getBatchesByMedicine(newVal.getMed_id());
                ObservableList<Batch> batchItems = FXCollections.observableArrayList(batches);
                FilteredList<Batch> filteredBatches = new FilteredList<>(batchItems, p -> true);
                
                batchCombo.setItems(filteredBatches);
                batchCombo.setDisable(false);
                resetFields();

                // إعداد البحث في الطبخات
                batchCombo.setConverter(new StringConverter<>() {
                    @Override public String toString(Batch b) { 
                        return b != null ? "رقم: " + b.getBatch_number() + " | صلاحية: " + b.getExp_date() : ""; 
                    }
                    @Override public Batch fromString(String string) {
                        return batchItems.stream().filter(b -> ("رقم: " + b.getBatch_number() + " | صلاحية: " + b.getExp_date()).equals(string)).findFirst().orElse(null);
                    }
                });

                // فلترة الطبخات برقم الطبخة
                batchCombo.getEditor().textProperty().addListener((bObs, bOld, bNew) -> {
                    final TextField bEditor = batchCombo.getEditor();
                    final Batch bSelected = batchCombo.getSelectionModel().getSelectedItem();
                    
                    String selectedStr = bSelected != null ? "رقم: " + bSelected.getBatch_number() + " | صلاحية: " + bSelected.getExp_date() : "";
                    if (bSelected != null && selectedStr.equals(bEditor.getText())) return;

                    filteredBatches.setPredicate(item -> {
                        if (bNew == null || bNew.trim().isEmpty()) return true;
                        return item.getBatch_number().toLowerCase().contains(bNew.toLowerCase());
                    });

                    if (!filteredBatches.isEmpty() && !batchCombo.isShowing() && batchCombo.getScene() != null) {
                        Platform.runLater(batchCombo::show);
                    }
                });
            }
        });

        // عند اختيار طبخة، إظهار الكمية الدفترية وحساب الفرق
        batchCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                systemQtyField.setText(String.valueOf(newVal.getQuantity()));
                calculateDifference();
                Platform.runLater(() -> actualQtyField.requestFocus()); // نقل المؤشر فوراً لكتابة الكمية الفعلية
            }
        });
    }

    // ==========================================
    // 2. الحسابات
    // ==========================================
    private void setupQuantityCalculations() {
        actualQtyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                actualQtyField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            calculateDifference();
        });
    }

    private void calculateDifference() {
        try {
            if (!systemQtyField.getText().isEmpty() && !actualQtyField.getText().isEmpty()) {
                int sysQty = Integer.parseInt(systemQtyField.getText());
                int actQty = Integer.parseInt(actualQtyField.getText());
                int diff = actQty - sysQty;
                
                differenceField.setText(String.valueOf(diff));
                
                if (diff < 0) differenceField.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                else if (diff > 0) differenceField.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                else differenceField.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
            } else {
                differenceField.clear();
            }
        } catch (NumberFormatException e) {
            differenceField.clear();
        }
    }

    private void resetFields() {
        batchCombo.getSelectionModel().clearSelection();
        batchCombo.getEditor().clear();
        systemQtyField.clear();
        actualQtyField.clear();
        differenceField.clear();
    }

    // ==========================================
    // 3. الحفظ والإلغاء
    // ==========================================
    @FXML
    private void handleSave(ActionEvent event) {
        try {
            Batch selectedBatch = batchCombo.getSelectionModel().getSelectedItem();
            String actualStr = actualQtyField.getText();
            String notes = notesArea.getText().trim();

            if (selectedBatch == null || actualStr.isEmpty() || notes.isEmpty()) {
                AlertManager.showError("بيانات ناقصة", "يرجى تحديد الطبخة، كتابة الكمية الفعلية، وذكر السبب.");
                return;
            }

            int actualQty = Integer.parseInt(actualStr);
            int sysQty = Integer.parseInt(systemQtyField.getText());
            int difference = actualQty - sysQty;

            if (difference == 0) {
                AlertManager.showError("تنبيه", "الكمية الفعلية تطابق الدفترية! لا توجد تسوية مطلوبة.");
                return;
            }

            InventoryAdjustment adjustment = new InventoryAdjustment();
            adjustment.setBatch_id(selectedBatch.getBatch_id());
            adjustment.setSystem_quantity(sysQty);
            adjustment.setNotes(notes);

            boolean success = adjustmentService.processAdjustmentTransaction(adjustment, actualQty);

            if (success) {
                AlertManager.showSuccess("تمت التسوية", "تم اعتماد الجرد وتعديل مخزون الطبخة بنجاح.");
                closeWindow(event);
            } else {
                AlertManager.showError("خطأ", "فشلت عملية التسوية بالكامل وتم التراجع عن أي تعديل.");
            }

        } catch (IllegalStateException | IllegalArgumentException e) {
            AlertManager.showError("تنبيه", e.getMessage());
        } catch (Exception e) {
            AlertManager.showError("خطأ تقني", "حدث خطأ غير متوقع: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow(event);
    }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}