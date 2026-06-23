package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.DisposalDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Disposal;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.DisposalServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.DisposalService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.utils.gui.AlertManager;

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

import java.math.BigDecimal;
import java.util.List;

public class AddDisposalController {

    @FXML private ComboBox<Medicine> medicineCombo;
    @FXML private ComboBox<Batch> batchCombo;
    @FXML private TextField costPerBoxField;
    @FXML private TextField qtyDisposedField;
    @FXML private TextField totalCostField;
    @FXML private TextField compensationField;
    @FXML private Label lblPharmacyLoss;
    @FXML private TextArea reasonArea;

    private MedicineService medicineService;
    private BatchService batchService;
    private DisposalService disposalService;

    private BigDecimal currentBoxCost = BigDecimal.ZERO;
    private BigDecimal calculatedTotalCost = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        batchService = new BatchServiceImpl(new BatchDAOImpl());
        disposalService = new DisposalServiceImpl(new DisposalDAOImpl());

        setupSearchableCombos();
        setupCalculations();
        
        Platform.runLater(() -> medicineCombo.requestFocus());
    }

    private void setupSearchableCombos() {
        // ==========================================
        // 1. البحث الذكي للأدوية
        // ==========================================
        ObservableList<Medicine> medItems = FXCollections.observableArrayList(medicineService.getActiveMedicines());
        FilteredList<Medicine> filteredMeds = new FilteredList<>(medItems, p -> true);
        medicineCombo.setItems(filteredMeds);

        medicineCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Medicine m) { return m != null ? m.getBrand_name() : ""; }
            @Override public Medicine fromString(String s) {
                return medItems.stream().filter(item -> item.getBrand_name().equals(s)).findFirst().orElse(null);
            }
        });

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

        // ==========================================
        // 2. البحث الذكي للطبخات (مبني على كودك الناجح)
        // ==========================================
        medicineCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<Batch> batches = batchService.getBatchesByMedicine(newVal.getMed_id());
                ObservableList<Batch> batchItems = FXCollections.observableArrayList(batches);
                FilteredList<Batch> filteredBatches = new FilteredList<>(batchItems, p -> true);
                
                batchCombo.setItems(filteredBatches);
                batchCombo.setDisable(false);
                resetCalculations();

                // عرض الطبخة
                batchCombo.setConverter(new StringConverter<>() {
                    @Override public String toString(Batch b) { 
                        return b != null ? "رقم: " + b.getBatch_number() + " | متوفر: " + b.getQuantity() : ""; 
                    }
                    @Override public Batch fromString(String string) {
                        return batchItems.stream().filter(b -> ("رقم: " + b.getBatch_number() + " | متوفر: " + b.getQuantity()).equals(string)).findFirst().orElse(null);
                    }
                });

                // فلترة الطبخات (برقم الطبخة)
                batchCombo.getEditor().textProperty().addListener((bObs, bOld, bNew) -> {
                    final TextField bEditor = batchCombo.getEditor();
                    final Batch bSelected = batchCombo.getSelectionModel().getSelectedItem();
                    
                    String selectedStr = bSelected != null ? "رقم: " + bSelected.getBatch_number() + " | متوفر: " + bSelected.getQuantity() : "";
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

        // ==========================================
        // 3. عند اختيار طبخة، حساب التكلفة والانتقال
        // ==========================================
        batchCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                currentBoxCost = newVal.getBuy_box_cost();
                costPerBoxField.setText(currentBoxCost.toString());
                updateLiveMath();
                Platform.runLater(() -> qtyDisposedField.requestFocus());
            }
        });
    }

    private void setupCalculations() {
        qtyDisposedField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) qtyDisposedField.setText(newVal.replaceAll("[^\\d]", ""));
            updateLiveMath();
        });

        compensationField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) compensationField.setText(old);
            updateLiveMath();
        });
    }

    private void updateLiveMath() {
        try {
            int qty = qtyDisposedField.getText().isEmpty() ? 0 : Integer.parseInt(qtyDisposedField.getText());
            calculatedTotalCost = currentBoxCost.multiply(new BigDecimal(qty));
            totalCostField.setText(calculatedTotalCost.toString());

            BigDecimal comp = compensationField.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(compensationField.getText());
            BigDecimal loss = calculatedTotalCost.subtract(comp);
            
            lblPharmacyLoss.setText(loss.toString() + " ل.س");
            if (loss.compareTo(BigDecimal.ZERO) < 0) {
                lblPharmacyLoss.setText("التعويض أكبر من التكلفة! يرجى المراجعة");
            }
        } catch (Exception e) {
            // تجاهل أخطاء التنسيق المؤقتة
        }
    }

    private void resetCalculations() {
        batchCombo.getSelectionModel().clearSelection();
        batchCombo.getEditor().clear();
        qtyDisposedField.clear();
        compensationField.clear();
        totalCostField.clear();
        lblPharmacyLoss.setText("0.00 ل.س");
        currentBoxCost = BigDecimal.ZERO;
    }

    @FXML
    private void handleSave(ActionEvent event) {
        Batch selectedBatch = batchCombo.getSelectionModel().getSelectedItem();
        if (selectedBatch == null || qtyDisposedField.getText().isEmpty() || reasonArea.getText().trim().isEmpty()) {
            AlertManager.showError("نقص بيانات", "يرجى اختيار الطبخة وإدخال الكمية المتلفة وسبب الإتلاف.");
            return;
        }

        int qty = Integer.parseInt(qtyDisposedField.getText());
        if (qty <= 0 || qty > selectedBatch.getQuantity()) {
            AlertManager.showError("خطأ في الكمية", "الكمية المتلفة تتجاوز الكمية المتوفرة في المستودع (" + selectedBatch.getQuantity() + ").");
            return;
        }

        boolean confirmed = AlertManager.showConfirmation("تأكيد الإتلاف", "هل أنت متأكد من إتلاف " + qty + " علبة؟");
        if (!confirmed) return;

        try {
            Disposal disposal = new Disposal();
            disposal.setBatch_id(selectedBatch.getBatch_id());
            disposal.setQuantity_disposed(qty);
            disposal.setTotal_cost(calculatedTotalCost);
            
            BigDecimal comp = compensationField.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(compensationField.getText());
            disposal.setSupplier_compensation_amount(comp);
            disposal.setReason(reasonArea.getText().trim());

            if (disposalService.createDisposal(disposal).isPresent()) {
                selectedBatch.setQuantity(selectedBatch.getQuantity() - qty);
                batchService.updateBatch(selectedBatch);

                AlertManager.showSuccess("تم بنجاح", "تم تسجيل الإتلاف وخصم الكمية بنجاح.");
                closeWindow(event);
            }
        } catch (Exception e) {
            AlertManager.showError("خطأ", "حدث خطأ أثناء الإتلاف: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) { closeWindow(event); }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}