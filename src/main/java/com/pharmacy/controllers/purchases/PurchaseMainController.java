package com.pharmacy.controllers.purchases;

import com.pharmacy.models.purchasing.PurchaseItem;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.services.interfaces.purchasing.SupplierService;
import com.pharmacy.dao.impl.inventory.*;
import com.pharmacy.dao.impl.purchasing.*;
import com.pharmacy.services.impl.inventory.*;
import com.pharmacy.services.impl.purchasing.*;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class PurchaseMainController implements Initializable {

    // ==========================================
    // 1. حقن الواجهة (FXML Injections)
    // ==========================================
    @FXML private BorderPane rootPane;

    // البحث
    @FXML private TextField medicineSearchField;

    // الجدول وأعمدته
    @FXML private TableView<PurchaseItem> purchaseTable;
    @FXML private TableColumn<PurchaseItem, String> barcodeCol;
    @FXML private TableColumn<PurchaseItem, String> nameCol;
    @FXML private TableColumn<PurchaseItem, String> batchNumberCol;
    @FXML private TableColumn<PurchaseItem, LocalDate> mfgDateCol;
    @FXML private TableColumn<PurchaseItem, LocalDate> expDateCol;
    @FXML private TableColumn<PurchaseItem, Integer> quantityCol;
    @FXML private TableColumn<PurchaseItem, Integer> bonusCol;
    @FXML private TableColumn<PurchaseItem, BigDecimal> boxCostCol;
    @FXML private TableColumn<PurchaseItem, BigDecimal> totalCostCol;
    @FXML private TableColumn<PurchaseItem, Void> actionCol;

    // بيانات الفاتورة والمورد
    @FXML private DatePicker purchaseDatePicker;
    @FXML private TextField supplierSearchField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextField paidAmountField;
    @FXML private TextField supplierInvoiceField;

    // الملخص المالي
    @FXML private Label totalInvoiceCostLabel;
    @FXML private Label netTotalLabel;
    @FXML private Label debtTitleLabel;
    @FXML private Label debtAmountLabel;

    // الأزرار
    @FXML private Button savePurchaseButton;
    @FXML private Button clearFormButton;

    // ==========================================
    // 2. متغيرات الحالة (State & Data Models)
    // ==========================================
    private ObservableList<PurchaseItem> purchaseItems;
    
    // الخصائص التفاعلية للعمليات المالية
    private ObjectProperty<BigDecimal> totalInvoiceProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> paidAmountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> debtAmountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // متغير لحفظ الـ ID الخاص بالمورد بعد اختياره من القائمة
    private Integer currentSupplierId = null;

    // أدوات الإكمال التلقائي
    private AutoCompletionBinding<String> medicineAutoCompletion;
    private AutoCompletionBinding<String> supplierAutoCompletion;

    // ==========================================
    // 3. الخدمات (Injected Services)
    // ==========================================
    private MedicineService medicineService;
    private BatchService batchService;
    private SupplierService supplierService;
    private PurchaseService purchaseService;
    private com.pharmacy.services.interfaces.purchasing.SupplierPaymentService supplierPaymentService;
    // ==========================================
    // 4. مرحلة الإقلاع والتهيئة (Initialization)
    // ==========================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. تهيئة قائمة السلة
        purchaseItems = FXCollections.observableArrayList();
        purchaseTable.setItems(purchaseItems);

        // 2. استدعاء دوال التهيئة
        injectServices();
        setupDefaults();
 
        setupTableColumns(); // سيتم إضافتها لاحقاً
        setupAutoCompletion(); // سيتم إضافتها لاحقاً
        setupFinancialBindings(); // سيتم إضافتها لاحقاً
        // setupListeners(); // سيتم إضافتها لاحقاً
    }

    private void injectServices() {
        this.medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        this.batchService = new BatchServiceImpl(new BatchDAOImpl());
        this.supplierService = new SupplierServiceImpl(new SupplierDAOImpl());
        this.purchaseService = new PurchaseServiceImpl(new PurchaseDAOImpl(), new PurchaseDetailDAOImpl());
        this.supplierPaymentService = new SupplierPaymentServiceImpl(new SupplierPaymentDAOImpl()); // تمت الإضافة
    }

    private void setupDefaults() {
        // تعيين تاريخ اليوم كافتراضي لفاتورة الشراء
        purchaseDatePicker.setValue(LocalDate.now());
        
        // تعبئة قائمة طرق الدفع للمورد
        paymentMethodCombo.getItems().addAll("آجل (ذمة مالية)", "نقدي", "دفعة من الحساب");
        paymentMethodCombo.getSelectionModel().selectFirst(); // آجل كافتراضي
    }

    private void setupAutoCompletion() {
        // 1. الإكمال التلقائي للأدوية (لإضافتها للفاتورة)
        medicineAutoCompletion = TextFields.bindAutoCompletion(medicineSearchField, request -> {
            String keyword = request.getUserText().trim();
            if (keyword.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            String searchPattern = "%" + keyword + "%";
            return medicineService.searchMedicines(searchPattern).stream()
                    .map(med -> med.getBrand_name() + " [" + med.getBarcode() + "]")
                    .collect(java.util.stream.Collectors.toList());
        });

        medicineAutoCompletion.setOnAutoCompleted(event -> {
            String selectedText = event.getCompletion();
            processMedicineInput(selectedText);
        });

        // 2. الإكمال التلقائي للموردين
        supplierAutoCompletion = TextFields.bindAutoCompletion(supplierSearchField, request -> {
            String keyword = request.getUserText().trim();
            if (keyword.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            String searchPattern = "%" + keyword + "%";
            return supplierService.searchSuppliers(searchPattern).stream()
                    .map(sup -> sup.getName() + " [" + sup.getSupplier_id() + "]")
                    .collect(java.util.stream.Collectors.toList());
        });

        // 🛡️ الحل: متغير محلي كـ (علم/Flag) لمنع تداخل الأحداث
        final boolean[] isProgrammaticChange = {false};

        supplierAutoCompletion.setOnAutoCompleted(event -> {
            isProgrammaticChange[0] = true; // إيقاف جدار الحماية مؤقتاً
            
            String selectedText = event.getCompletion();
            int start = selectedText.lastIndexOf("[");
            int end = selectedText.lastIndexOf("]");
            
            if (start != -1 && end != -1 && start < end) {
                try {
                    currentSupplierId = Integer.parseInt(selectedText.substring(start + 1, end));
                } catch (NumberFormatException e) {
                    currentSupplierId = null;
                }
                // هذا السطر كان يخدع جدار الحماية، الآن هو آمن
                supplierSearchField.setText(selectedText.substring(0, start).trim());
            }
            
            isProgrammaticChange[0] = false; // إعادة تشغيل جدار الحماية
        });

        supplierSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // إذا كان التعديل برمجياً، نتجاهل الأمر. إذا كان المستخدم يكتب يدوياً، نفرغ الـ ID
            if (!isProgrammaticChange[0]) {
                currentSupplierId = null; 
            }
        });
    }
    
    
    // ==========================================
    // 5. تفاعلات المستخدم (Event Handlers)
    // ==========================================
    
    // ==========================================
// 5. تفاعلات المستخدم (Event Handlers)
// ==========================================

// 1. هذه الدالة المربوطة بالـ FXML وتصطاد ضغطة Enter من السكانر أو الكيبورد
    @FXML
    private void handleBarcodeScan(ActionEvent event) {
        processMedicineInput(medicineSearchField.getText());
    }

    // 2. العقل المدبر: يحلل النص ويقرر هل هو باركود أم اسم، ثم يضيفه للجدول
    private void processMedicineInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String query = input.trim();
        java.util.Optional<com.pharmacy.models.inventory.Medicine> optMedicine = java.util.Optional.empty();

        // -------------------------------------------------------------
        // 1. الفلترة الذكية (استخراج الباركود أو الاسم)
        // -------------------------------------------------------------
        if (query.matches("\\d+")) {
            // حالة مسح الباركود مباشرة
            optMedicine = medicineService.getMedicineByBarcode(query);
        } else if (query.contains("[") && query.contains("]")) {
            // حالة اختيار الدواء من القائمة المنسدلة (AutoComplete)
            int start = query.lastIndexOf("[");
            int end = query.lastIndexOf("]");
            if (start != -1 && end != -1 && start < end) {
                String barcode = query.substring(start + 1, end).trim();
                optMedicine = medicineService.getMedicineByBarcode(barcode);
            }
        }

        // إذا لم يجده بالباركود، نجري بحثاً بالاسم (لتغطية الإدخال اليدوي والنقر على Enter)
        if (!optMedicine.isPresent()) {
            java.util.List<com.pharmacy.models.inventory.Medicine> searchResults = medicineService.searchMedicines(query);
            if (searchResults.size() == 1) {
                optMedicine = java.util.Optional.of(searchResults.get(0));
            } else if (searchResults.size() > 1) {
                showAlert("تنبيه", "يوجد أكثر من دواء بهذا الاسم. يرجى اختياره من القائمة بدقة.", Alert.AlertType.INFORMATION);
                return;
            }
        }

        // التحقق النهائي من وجود الدواء
        if (!optMedicine.isPresent()) {
            showAlert("دواء غير معرف", "هذا الدواء غير موجود في الكتالوج. يرجى تعريفه من شاشة إدارة الأدوية أولاً.", Alert.AlertType.WARNING);
            medicineSearchField.selectAll(); // تظليل النص ليسهل مسحه
            return;
        }

        com.pharmacy.models.inventory.Medicine medicine = optMedicine.get();

        // -------------------------------------------------------------
        // الميزة 1: منع تكرار الدواء في الفاتورة (نجمع الكميات فقط)
        // -------------------------------------------------------------
        for (PurchaseItem item : purchaseItems) { 
            if (item.getMedId() == medicine.getMed_id()) {
                item.setQuantity(item.getQuantity() + 1); // نزيد الكمية حبة واحدة
                purchaseTable.refresh(); // نحدث شكل الجدول
                updateFinancialTotals(); // نحدث إجمالي الفاتورة

                // إفراغ الحقل لانتظار الباركود التالي
                medicineSearchField.clear(); 
                medicineSearchField.requestFocus();
                return; 
            }
        }

        // -------------------------------------------------------------
        // الميزة 2: البحث عن آخر طبخة وجلب بياناتها (الملء التلقائي)
        // -------------------------------------------------------------
        String defaultBatch = ""; 
        java.time.LocalDate defaultMfgDate = java.time.LocalDate.now();
        java.time.LocalDate defaultExpDate = java.time.LocalDate.now().plusYears(2);
        java.math.BigDecimal defaultBoxCost = java.math.BigDecimal.ZERO;

        java.util.Optional<com.pharmacy.models.inventory.Batch> lastBatchOpt = batchService.getLastBatchByMedicineId(medicine.getMed_id());

        if (lastBatchOpt.isPresent()) {
            com.pharmacy.models.inventory.Batch lastBatch = lastBatchOpt.get();
            defaultBatch = lastBatch.getBatch_number();
            defaultMfgDate = lastBatch.getMfg_date();
            defaultExpDate = lastBatch.getExp_date();
            defaultBoxCost = lastBatch.getBuy_box_cost();
        }

        int defaultQuantity = 1;
        int defaultBonus = 0;

        PurchaseItem newItem = new PurchaseItem(
                medicine.getMed_id(),
                medicine.getBarcode(),
                medicine.getBrand_name(),
                defaultBatch,
                defaultMfgDate,
                defaultExpDate,
                defaultQuantity,
                defaultBonus,
                defaultBoxCost
        );

        // إضافة السطر للجدول
        purchaseItems.add(newItem);
        updateFinancialTotals();

        // إفراغ الحقل لانتظار الدواء التالي
        medicineSearchField.clear();
        medicineSearchField.requestFocus();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ==========================================
    // 6. إعدادات الجدول (Table Columns Mapping & Editing)
    // ==========================================
    
    // ==========================================
    // 6. إعدادات الجدول (Table Columns Mapping & Editing)
    // ==========================================
    
    private void setupTableColumns() {
        // =========================================================
        // 0. توسيط الكلام في المنتصف لجميع الأعمدة
        // =========================================================
        barcodeCol.setStyle("-fx-alignment: CENTER;");
        nameCol.setStyle("-fx-alignment: CENTER;");
        batchNumberCol.setStyle("-fx-alignment: CENTER;");
        mfgDateCol.setStyle("-fx-alignment: CENTER;");
        expDateCol.setStyle("-fx-alignment: CENTER;");
        quantityCol.setStyle("-fx-alignment: CENTER;");
        bonusCol.setStyle("-fx-alignment: CENTER;");
        boxCostCol.setStyle("-fx-alignment: CENTER;");
        totalCostCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setStyle("-fx-alignment: CENTER;");

        // 1. تفعيل وضع التعديل للجدول
        purchaseTable.setEditable(true);
        
        // 2. ربط الأعمدة غير القابلة للتعديل
        barcodeCol.setCellValueFactory(cellData -> cellData.getValue().barcodeProperty());
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        totalCostCol.setCellValueFactory(cellData -> cellData.getValue().totalCostProperty());

        // ---------------------------------------------------------
        // 3. عمود رقم الطبخة (Batch Number) - إجباري وذكي (مع إكمال تلقائي)
        // ---------------------------------------------------------
        batchNumberCol.setCellValueFactory(cellData -> cellData.getValue().batchNumberProperty());
        batchNumberCol.setCellFactory(column -> new TableCell<PurchaseItem, String>() {
            private TextField textField;
            
            // التصحيح هنا: المتغير يجب أن يكون من نوع Batch وليس String
            private org.controlsfx.control.textfield.AutoCompletionBinding<com.pharmacy.models.inventory.Batch> autoCompletionBinding;

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();
                    if (textField == null) {
                        createTextField();
                    }
                    setText(null);
                    setGraphic(textField);
                    textField.selectAll();
                    
                    // التركيز التلقائي عند بدء التعديل
                    javafx.application.Platform.runLater(() -> textField.requestFocus());
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            textField.setText(getString());
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(getString());
                        setGraphic(null);
                    }
                }
            }

            private String getString() {
                return getItem() == null ? "" : getItem();
            }

            private void createTextField() {
                textField = new TextField(getString());
                
                // إضافة توسيط النص داخل حقل الإكمال التلقائي أثناء الكتابة
                textField.setStyle("-fx-alignment: CENTER;"); 
                
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                PurchaseItem currentItem = getTableView().getItems().get(getIndex());
                int currentMedId = currentItem.getMedId();

                // ربط الإكمال التلقائي بجدول الطبخات
                autoCompletionBinding = org.controlsfx.control.textfield.TextFields.bindAutoCompletion(textField, request -> {
                    String keyword = request.getUserText().trim().toLowerCase();
                    return batchService.getBatchesByMedicine(currentMedId).stream()
                            .filter(batch -> batch.getBatch_number().toLowerCase().contains(keyword))
                            .collect(java.util.stream.Collectors.toList());
                });

                // تعبئة البيانات تلقائياً
                autoCompletionBinding.setOnAutoCompleted(event -> {
                    com.pharmacy.models.inventory.Batch selectedBatch = event.getCompletion();
                    if (selectedBatch != null) {
                        textField.setText(selectedBatch.getBatch_number());
                        
                        currentItem.setMfgDate(selectedBatch.getMfg_date());
                        currentItem.setExpDate(selectedBatch.getExp_date());
                        currentItem.setBoxCost(selectedBatch.getBuy_box_cost());
                        
                        purchaseTable.refresh(); 
                        updateFinancialTotals();
                    }
                });

                textField.setOnAction(e -> commitEdit(textField.getText()));
                textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        commitEdit(textField.getText());
                    }
                });
            }
        });
        
        batchNumberCol.setOnEditCommit(event -> {
            event.getRowValue().setBatchNumber(event.getNewValue());
        });

        // ---------------------------------------------------------
        // 4. الحل الهندسي للتواريخ: استخدام DatePicker داخل الجدول
        // ---------------------------------------------------------
        mfgDateCol.setCellValueFactory(cellData -> cellData.getValue().mfgDateProperty());
        mfgDateCol.setCellFactory(createDatePickerCellFactory());
        mfgDateCol.setOnEditCommit(event -> {
            event.getRowValue().setMfgDate(event.getNewValue());
        });

        expDateCol.setCellValueFactory(cellData -> cellData.getValue().expDateProperty());
        expDateCol.setCellFactory(createDatePickerCellFactory());
        expDateCol.setOnEditCommit(event -> {
            event.getRowValue().setExpDate(event.getNewValue());
        });

        // 5. عمود الكمية - قابل للتعديل
        quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());
        quantityCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        quantityCol.setOnEditCommit(event -> {
            Integer newVal = event.getNewValue();
            if (newVal != null && newVal > 0) {
                event.getRowValue().setQuantity(newVal);
                updateFinancialTotals(); 
            } else {
                purchaseTable.refresh(); 
            }
        });

        // 6. عمود البونص - قابل للتعديل
        bonusCol.setCellValueFactory(cellData -> cellData.getValue().bonusProperty().asObject());
        bonusCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        bonusCol.setOnEditCommit(event -> {
            Integer newVal = event.getNewValue();
            if (newVal != null && newVal >= 0) {
                event.getRowValue().setBonus(newVal);
            } else {
                purchaseTable.refresh();
            }
        });

        // 7. عمود سعر الشراء - قابل للتعديل
        boxCostCol.setCellValueFactory(cellData -> cellData.getValue().boxCostProperty());
        boxCostCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.BigDecimalStringConverter()));
        boxCostCol.setOnEditCommit(event -> {
            BigDecimal newVal = event.getNewValue();
            if (newVal != null && newVal.compareTo(BigDecimal.ZERO) >= 0) {
                event.getRowValue().setBoxCost(newVal);
                updateFinancialTotals(); 
            } else {
                purchaseTable.refresh();
            }
        });

        // 8. عمود الحذف
        actionCol.setCellFactory(param -> new TableCell<PurchaseItem, Void>() {
            private final Button deleteBtn = new Button("حذف");

            {
                // ستايل أحمر أنيق وحجم صغير متطابق مع الصورة
                String normalStyle = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4px; -fx-padding: 5;";
                String hoverStyle =  "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4px; -fx-padding: 5;";

                deleteBtn.setStyle(normalStyle);
                deleteBtn.setCursor(javafx.scene.Cursor.HAND);
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(hoverStyle));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(normalStyle));

                deleteBtn.setPrefWidth(55);  
                deleteBtn.setPrefHeight(25);
                deleteBtn.setMinWidth(55);
                deleteBtn.setMaxHeight(25);

                deleteBtn.setOnAction(event -> {
                    PurchaseItem item = getTableView().getItems().get(getIndex());
                    purchaseItems.remove(item);
                    updateFinancialTotals(); // دالتك لتحديث الحسابات
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(deleteBtn);
            }
        });
        
        purchaseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // ==========================================
    // 7. العمليات الحسابية والمالية
    // ==========================================

    private void setupFinancialBindings() {
        // ربط الخصائص بالنصوص الظاهرة على الشاشة
        totalInvoiceCostLabel.textProperty().bind(totalInvoiceProperty.asString("%.2f"));
        netTotalLabel.textProperty().bind(totalInvoiceProperty.asString("%.2f")); 
        //debtAmountLabel.textProperty().bind(debtAmountProperty.asString("%.2f"));

        // مراقبة التغييرات لحساب الذمة المتبقية فوراً
        paidAmountField.textProperty().addListener((obs, oldVal, newVal) -> calculateDebt());
        totalInvoiceProperty.addListener((obs, oldVal, newVal) -> calculateDebt());
        paymentMethodCombo.valueProperty().addListener((obs, oldVal, newVal) -> handlePaymentMethodChange(newVal));
    }

    private void updateFinancialTotals() {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItem item : purchaseItems) {
            if (item.getTotalCost() != null) {
                total = total.add(item.getTotalCost());
            }
        }
        totalInvoiceProperty.set(total);
    }

    private void handlePaymentMethodChange(String method) {
        boolean isCashOrPartial = "نقدي".equals(method) || "دفعة من الحساب".equals(method);
        
        paidAmountField.setVisible(isCashOrPartial);
        paidAmountField.setManaged(isCashOrPartial);
        
        if ("آجل (ذمة مالية)".equals(method)) {
            paidAmountField.clear();
        } else if ("نقدي".equals(method)) {
            // إذا اختار نقدي، نقوم بملء حقل "المبلغ المدفوع" بإجمالي الفاتورة تلقائياً لتسريع العمل
            paidAmountField.setText(totalInvoiceProperty.get().toString());
        }
        calculateDebt();
    }
    
    // ==========================================
    // 8. التحقق والحفظ (Validation & Saving)
    // ==========================================

    private boolean validatePurchase() {
        // 1. التحقق من وجود أدوية في الفاتورة
        if (purchaseItems.isEmpty()) {
            showAlert("فاتورة فارغة", "لا يمكن حفظ فاتورة لا تحتوي على مواد. يرجى إضافة أدوية أولاً.", Alert.AlertType.WARNING);
            return false;
        }

        // 2. التحقق من المورد (إجباري)
        if (currentSupplierId == null || supplierSearchField.getText().trim().isEmpty()) {
            showAlert("بيانات ناقصة", "يرجى اختيار المورد من القائمة المنسدلة.", Alert.AlertType.WARNING);
            supplierSearchField.requestFocus();
            return false;
        }

        // 3. التحقق من رقم فاتورة المورد (إجباري)
        if (supplierInvoiceField.getText() == null || supplierInvoiceField.getText().trim().isEmpty()) {
            showAlert("بيانات ناقصة", "يرجى إدخال رقم فاتورة المورد.", Alert.AlertType.WARNING);
            supplierInvoiceField.requestFocus();
            return false;
        }

        // 4. التحقق المالي (إذا كان الدفع نقدي، يجب أن يغطي المبلغ المدفوع إجمالي الفاتورة)
        String paymentMethod = paymentMethodCombo.getValue();
        if ("نقدي".equals(paymentMethod)) {
            BigDecimal total = totalInvoiceProperty.get();
            String paidText = paidAmountField.getText();
            BigDecimal paid = (paidText == null || paidText.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(paidText.trim());

            if (paid.compareTo(total) < 0) {
                showAlert("نقص في الدفع", "في حالة الدفع النقدي، يجب أن يكون المبلغ المدفوع مساوياً لإجمالي الفاتورة.", Alert.AlertType.ERROR);
                paidAmountField.requestFocus();
                return false;
            }
        }

        // 5. التحقق من تواريخ الصلاحية داخل الجدول (لا يمكن إدخال دواء منتهي الصلاحية)
        for (PurchaseItem item : purchaseItems) {
            if (item.getExpDate().isBefore(LocalDate.now()) || item.getExpDate().isEqual(LocalDate.now())) {
                showAlert("تاريخ صلاحية غير صالح", "الدواء: " + item.getName() + " منتهي الصلاحية أو تنتهي صلاحيته اليوم! لا يمكن إدخاله للمستودع.", Alert.AlertType.ERROR);
                return false;
            }
        }
        
        // 5. التحقق من تواريخ الصلاحية ورقم الطبخة داخل الجدول
        for (PurchaseItem item : purchaseItems) {
            // التحقق من رقم الطبخة (إجباري)
            if (item.getBatchNumber() == null || item.getBatchNumber().trim().isEmpty()) {
                showAlert("بيانات ناقصة", "رقم الطبخة (Batch) إجباري للدواء: " + item.getName() + ".\nيرجى النقر على الخلية الفارغة في الجدول وكتابة الرقم.", Alert.AlertType.ERROR);
                return false;
            }

            // التحقق من تاريخ الصلاحية
            if (item.getExpDate().isBefore(LocalDate.now()) || item.getExpDate().isEqual(LocalDate.now())) {
                showAlert("تاريخ صلاحية غير صالح", "الدواء: " + item.getName() + " منتهي الصلاحية أو تنتهي صلاحيته اليوم! لا يمكن إدخاله للمستودع.", Alert.AlertType.ERROR);
                return false;
            }
        }
        return true;
    }
    private void calculateDebt() {
        try {
            BigDecimal total = totalInvoiceProperty.get() != null ? totalInvoiceProperty.get() : BigDecimal.ZERO;
            String method = paymentMethodCombo.getValue();
            
            if ("نقدي".equals(method)) {
                debtAmountProperty.set(BigDecimal.ZERO);
                debtTitleLabel.setText("حالة الدفع:");
                debtAmountLabel.setText("مدفوعة نقداً بالكامل");
                return;
            }

            String paidText = paidAmountField.getText();
            BigDecimal paid = (paidText == null || paidText.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(paidText.trim());

            // جدار أمني: منع إدخال دفعة أكبر من قيمة الفاتورة
            if (paid.compareTo(total) > 0) {
                paid = total;
                final String maxValStr = total.toString();
                javafx.application.Platform.runLater(() -> {
                    paidAmountField.setText(maxValStr);
                    paidAmountField.positionCaret(maxValStr.length());
                });
            }

            // حساب الذمة (الإجمالي - المدفوع)
            BigDecimal debt = total.subtract(paid);
            debtAmountProperty.set(debt);
            debtTitleLabel.setText("الذمة للمورد:");
            debtAmountLabel.setText(debt.toString());

        } catch (NumberFormatException e) {
            debtAmountLabel.setText("إدخال غير صالح");
        }
    }
    
    
    private javafx.util.Callback<TableColumn<PurchaseItem, LocalDate>, TableCell<PurchaseItem, LocalDate>> createDatePickerCellFactory() {
        return column -> new TableCell<PurchaseItem, LocalDate>() {
            private DatePicker datePicker;

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();
                    createDatePicker();
                    setText(null);
                    setGraphic(datePicker);
                    datePicker.requestFocus();
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() != null ? getItem().toString() : null);
                setGraphic(null);
            }

            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (datePicker != null) {
                            datePicker.setValue(getItem());
                        }
                        setText(null);
                        setGraphic(datePicker);
                    } else {
                        setText(getItem() != null ? getItem().toString() : null);
                        setGraphic(null);
                    }
                }
            }

            private void createDatePicker() {
                datePicker = new DatePicker(getItem());
                datePicker.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                
                // حفظ التعديل عند اختيار تاريخ من الروزنامة
                datePicker.setOnAction((e) -> {
                    commitEdit(datePicker.getValue());
                });
                
                // حفظ التعديل إذا قام المستخدم بالنقر خارج الخلية
                datePicker.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue) {
                        commitEdit(datePicker.getValue());
                    }
                });
            }
        };
    }
    // --- مؤقتاً لتجنب أخطاء الـ FXML ---
    // ==========================================
    // 8. الحفظ والتفريغ (Save & Clear)
    // ==========================================

    @FXML
    private void handleSavePurchase(ActionEvent event) {
        // 1. الجدار الأمني: التحقق من البيانات
        if (!validatePurchase()) {
            return; 
        }

        try {
            // 2. إنشاء وحفظ رأس الفاتورة (Purchase Header)
            com.pharmacy.models.purchasing.Purchase purchase = new com.pharmacy.models.purchasing.Purchase();
            purchase.setSupplier_id(currentSupplierId);
            // يتم جلب shift_id تلقائياً داخل الـ Service
            purchase.setPurchase_date(purchaseDatePicker.getValue().atStartOfDay());
            purchase.setTotal_cost(totalInvoiceProperty.get());
            purchase.setSupplier_invoice_number(supplierInvoiceField.getText().trim());
            
            String paymentMethod = paymentMethodCombo.getValue();
            purchase.setPayment_status("آجل (ذمة مالية)".equals(paymentMethod) ? "Unpaid" : "Paid");

            java.util.Optional<com.pharmacy.models.purchasing.Purchase> savedPurchaseOpt = purchaseService.createPurchase(purchase);
            
            if (!savedPurchaseOpt.isPresent()) {
                showAlert("خطأ في الحفظ", "حدث خطأ أثناء حفظ الفاتورة في قاعدة البيانات.", Alert.AlertType.ERROR);
                return;
            }
            
            com.pharmacy.models.purchasing.Purchase savedPurchase = savedPurchaseOpt.get();

            // 3. حفظ الدفعة المالية (إذا كان هناك دفع)
            if ("نقدي".equals(paymentMethod) || "دفعة من الحساب".equals(paymentMethod)) {
                BigDecimal paidAmount = new BigDecimal(paidAmountField.getText().trim());
                
                if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                    com.pharmacy.models.purchasing.SupplierPayment payment = new com.pharmacy.models.purchasing.SupplierPayment();
                    payment.setSupplier_id(currentSupplierId);
                    payment.setPurchase_id(savedPurchase.getPurchase_id());
                    
                    // تأكد أن الوردية ليست null
                    if (com.pharmacy.security.SessionManager.getInstance().getCurrentShift() != null) {
                        payment.setShift_id(com.pharmacy.security.SessionManager.getInstance().getCurrentShift().getShift_id());
                    } else {
                        throw new RuntimeException("لا توجد وردية مفتوحة! لا يمكن تسجيل الدفعة المالية.");
                    }
                    
                    payment.setAmount_paid(paidAmount);
                    payment.setPayment_date(LocalDateTime.now());
                    payment.setPayment_method(paymentMethod);
                    payment.setReceipt_number("INV-" + savedPurchase.getPurchase_id()); // رقم إيصال تلقائي
                    
                    // 🛡️ التعديل هنا: فحص النتيجة لمنع الخطأ الصامت
                    java.util.Optional<com.pharmacy.models.purchasing.SupplierPayment> savedPaymentOpt = supplierPaymentService.createPayment(payment);
                    
                    if (!savedPaymentOpt.isPresent()) {
                        throw new RuntimeException("فشل حفظ الدفعة المالية في قاعدة البيانات!\nيرجى مراجعة شاشة الـ Console لمعرفة سبب رفض SQLite للدفعة.");
                    }
                }
            }

            // 4. معالجة الطبخات (Batches) وتفاصيل الفاتورة (Purchase Details)
            // 4. معالجة الطبخات وتفاصيل الفاتورة (السحر المعماري لمعامل التحويل)
            for (PurchaseItem item : purchaseItems) {
                
                // 1. جلب بيانات الدواء لمعرفة معامل التحويل (كم ظرف في العلبة؟)
                com.pharmacy.models.inventory.Medicine medicine = medicineService.getMedicineById(item.getMedId())
                        .orElseThrow(() -> new RuntimeException("حدث خطأ: لم يتم العثور على الدواء رقم " + item.getMedId()));
                
                int conversionFactor = medicine.getConversion_factor();
                
                // 2. تحويل الكمية والبونص من (علب) إلى (الوحدة الأساسية/ظروف)
                int totalBoxes = item.getQuantity() + item.getBonus();
                int totalBaseUnits = totalBoxes * conversionFactor; 

                com.pharmacy.models.inventory.Batch targetBatch = null;
                
                // 3. التحقق مما إذا كانت هذه الطبخة موجودة مسبقاً
                java.util.Optional<com.pharmacy.models.inventory.Batch> existingBatchOpt = 
                        batchService.getBatchByNumberAndMedicine(item.getBatchNumber(), item.getMedId());
                
                if (existingBatchOpt.isPresent()) {
                    com.pharmacy.models.inventory.Batch existing = existingBatchOpt.get();
                    if (existing.getBuy_box_cost().compareTo(item.getBoxCost()) == 0 &&
                        existing.getExp_date().isEqual(item.getExpDate()) &&
                        existing.getMfg_date().isEqual(item.getMfgDate())) {
                        
                        targetBatch = existing;
                        // 🛡️ التحديث في المستودع يتم بالوحدة الأساسية (الظروف)
                        targetBatch.setQuantity(targetBatch.getQuantity() + totalBaseUnits);
                        batchService.updateBatch(targetBatch);
                    }
                }
                
                // 4. إذا لم تكن موجودة، ننشئ طبخة جديدة
                if (targetBatch == null) {
                    targetBatch = new com.pharmacy.models.inventory.Batch();
                    targetBatch.setMed_id(item.getMedId());
                    targetBatch.setBatch_number(item.getBatchNumber());
                    targetBatch.setMfg_date(item.getMfgDate());
                    targetBatch.setExp_date(item.getExpDate());
                    
                    // 🛡️ التخزين في المستودع يتم بالوحدة الأساسية (الظروف)
                    targetBatch.setQuantity(totalBaseUnits); 
                    
                    // التكلفة تبقى للعلبة كما هي للمحاسبة
                    targetBatch.setBuy_box_cost(item.getBoxCost()); 
                    targetBatch.setIs_active(1);
                    
                    targetBatch = batchService.createBatch(targetBatch)
                            .orElseThrow(() -> new RuntimeException("فشل إنشاء الطبخة في قاعدة البيانات"));
                }

                // 5. حفظ سطر الفاتورة (هنا نحتفظ بالكمية كـ "علب" لمطابقة فاتورة المورد الورقية)
                com.pharmacy.models.purchasing.PurchaseDetail detail = new com.pharmacy.models.purchasing.PurchaseDetail();
                detail.setPurchase_id(savedPurchase.getPurchase_id());
                detail.setBatch_id(targetBatch.getBatch_id());
                detail.setQuantity_received(item.getQuantity()); // علب
                detail.setBonus_quantity(item.getBonus());       // علب
                detail.setBox_cost(item.getBoxCost());           // سعر العلبة
                
                java.util.Optional<com.pharmacy.models.purchasing.PurchaseDetail> savedDetailOpt = purchaseService.addPurchaseDetail(detail);

                if (!savedDetailOpt.isPresent()) {
                    throw new RuntimeException("فشل إدراج تفاصيل الفاتورة للدواء: " + item.getName() + ".\nيرجى مراجعة شاشة الـ Console.");
                }
            }

            // 5. إعلام المستخدم والتهيئة للفاتورة القادمة
            showAlert("عملية ناجحة", "تم حفظ فاتورة المشتريات برقم: " + savedPurchase.getPurchase_id() + "، وتم إدراج الأدوية في المستودع بنجاح.", Alert.AlertType.INFORMATION);
            handleClearForm(null);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("خطأ النظام", "حدث خطأ أثناء معالجة الفاتورة: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClearForm(ActionEvent event) {
        // تفريغ الجدول
        purchaseItems.clear();
        updateFinancialTotals();
        
        // تفريغ الحقول
        supplierSearchField.clear();
        supplierInvoiceField.clear();
        currentSupplierId = null;
        medicineSearchField.clear();
        purchaseDatePicker.setValue(LocalDate.now());
        paymentMethodCombo.getSelectionModel().selectFirst();
        
        // إرجاع التركيز لحقل البحث
        medicineSearchField.requestFocus();
    }
}