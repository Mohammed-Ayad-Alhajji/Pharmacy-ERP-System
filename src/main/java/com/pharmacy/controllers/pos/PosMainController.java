package com.pharmacy.controllers.pos;

import com.pharmacy.models.pos.CartItem;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.SaleService;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import com.pharmacy.dao.impl.inventory.*;
import com.pharmacy.dao.impl.pos.*;
import com.pharmacy.services.impl.inventory.*;
import com.pharmacy.services.impl.pos.*;
import org.controlsfx.control.textfield.TextFields;
import java.util.stream.Collectors;
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
import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import java.util.Optional;
import java.util.List;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.inventory.Batch;
import java.io.File;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.stage.FileChooser;
import javafx.util.converter.BigDecimalStringConverter;

public class PosMainController implements Initializable {

    // ==========================================
    // 1. حقن الواجهة (FXML Injections)
    // ==========================================
    @FXML private BorderPane rootPane;

    // أدوات البحث
    @FXML private TextField searchField;
    
    // الجدول وأعمدته
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> barcodeCol;
    @FXML private TableColumn<CartItem, String> nameCol;
    @FXML private TableColumn<CartItem, String> batchCol;
    @FXML private TableColumn<CartItem, String> unitCol;
    @FXML private TableColumn<CartItem, BigDecimal> priceCol;
    @FXML private TableColumn<CartItem, Integer> quantityCol;
    @FXML private TableColumn<CartItem, BigDecimal> insuranceDiscountCol;
    @FXML private TableColumn<CartItem, BigDecimal> totalCol;
    @FXML private TableColumn<CartItem, Void> actionCol;

    // إعدادات الدفع والتأمين
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextField customerSearchField;
    @FXML private TextField insuranceSearchField;
    @FXML private TextField insuranceApprovalField;
    @FXML private Button attachPrescriptionButton;

    // الملخص المالي
    @FXML private Label totalAmountLabel;
    @FXML private Label totalCustomerDebtLabel;
    @FXML private Label totalInsuranceDebtLabel;
    @FXML private TextField discountField;
    @FXML private Label netTotalLabel;
    
    // حالة الدفع والباقي
    @FXML private TextField paidAmountField;
    @FXML private Label changeTitleLabel;
    @FXML private Label changeAmountLabel;

    // أزرار التحكم الرئيسية
    @FXML private Button checkoutButton;
    @FXML private Button clearCartButton;

    // ==========================================
    // 2. متغيرات الحالة (State & Data Models)
    // ==========================================
    
    // قائمة السلة المرتبطة بالجدول
    private ObservableList<CartItem> cartItems;
    
    // الخصائص التفاعلية للعمليات المالية (Reactive Properties)
    private ObjectProperty<BigDecimal> subTotalProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> discountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> roundingProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> netTotalProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> paidAmountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> insuranceDebtProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private ObjectProperty<BigDecimal> customerDebtProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // متغيرات حفظ بيانات الوصفة الطبية مؤقتاً
    private String currentDoctorName = null;
    private String currentPatientName = null;
    private String currentPrescriptionImagePath = null;

    // أدوات الإكمال التلقائي
    private AutoCompletionBinding<String> medicineAutoCompletion;
    private AutoCompletionBinding<String> customerAutoCompletion;
    private AutoCompletionBinding<String> insuranceAutoCompletion;

    // ==========================================
    // 3. الخدمات (Injected Services)
    // ==========================================
    private MedicineService medicineService;
    private BatchService batchService;
    private LocalCustomerService customerService;
    private InsuranceCompanyService insuranceService;
    private SaleService saleService;

    // ==========================================
    // 4. مرحلة الإقلاع والتهيئة (Initialization)
    // ==========================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. تهيئة قائمة السلة
        cartItems = FXCollections.observableArrayList();
        cartTable.setItems(cartItems);

        // 2. استدعاء دوال التهيئة
        injectServices();
        setupTableColumns();
        setupFinancialBindings();
        setupAutoCompletion();
        setupListeners();
    }

    /**
     * تهيئة حقن الخدمات من طبقة الـ Services.
     */
    private void injectServices() {
        this.medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        this.batchService = new BatchServiceImpl(new BatchDAOImpl());
        this.customerService = new LocalCustomerServiceImpl(new LocalCustomerDAOImpl());
        this.insuranceService = new InsuranceCompanyServiceImpl(new InsuranceCompanyDAOImpl());
        this.saleService = new SaleServiceImpl(new SaleDAOImpl(), new SaleDetailDAOImpl());
    }

    /**
     * ربط أعمدة الجدول بخصائص كائن CartItem وإعداد الـ CellFactories للتعديل والحذف.
     */
    private void setupTableColumns() {
        barcodeCol.setStyle("-fx-alignment: CENTER;");
        nameCol.setStyle("-fx-alignment: CENTER;");
        batchCol.setStyle("-fx-alignment: CENTER;");
        unitCol.setStyle("-fx-alignment: CENTER;");
        priceCol.setStyle("-fx-alignment: CENTER;");
        quantityCol.setStyle("-fx-alignment: CENTER;"); // هام جداً لتوسيط حقل إدخال الكمية
        insuranceDiscountCol.setStyle("-fx-alignment: CENTER;");
        totalCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setStyle("-fx-alignment: CENTER;");
        // 1. السماح بتعديل الجدول
        cartTable.setEditable(true);

        // 2. ربط الأعمدة بالخصائص الموجودة في CartItem
        barcodeCol.setCellValueFactory(cellData -> cellData.getValue().barcodeProperty());
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        batchCol.setCellValueFactory(cellData -> cellData.getValue().batchNumberProperty());
        unitCol.setCellValueFactory(cellData -> cellData.getValue().unitProperty());
        priceCol.setCellValueFactory(cellData -> cellData.getValue().priceProperty());
        insuranceDiscountCol.setCellValueFactory(cellData -> cellData.getValue().insuranceDiscountProperty());
        totalCol.setCellValueFactory(cellData -> cellData.getValue().subTotalProperty());

       // 3. إعداد عمود الكمية ليكون قابلاً للتعديل
        quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());
        quantityCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        
        // معالجة حدث تغيير الكمية (مع خوارزمية الانقسام التلقائي للدفعات)
        quantityCol.setOnEditCommit(event -> {
            CartItem item = event.getRowValue();
            Integer newQuantity = event.getNewValue();

            // حماية من الإدخالات الخاطئة أو السالبة
            if (newQuantity == null || newQuantity <= 0) {
                cartTable.refresh(); 
                return;
            }

            Optional<Medicine> optMed = medicineService.getMedicineByBarcode(item.getBarcode());
            Optional<Batch> optBatch = batchService.getBatchById(item.getBatchId());

            if (optMed.isPresent() && optBatch.isPresent()) {
                Medicine med = optMed.get();
                Batch currentBatch = optBatch.get();

                // حساب الكمية المطلوبة بالوحدة الصغرى (الظرف)
                int conversionFactor = item.getUnit().equals("علبة") ? med.getConversion_factor() : 1;
                int requestedStripsForThisRow = newQuantity * conversionFactor;

                // الحالة الأولى: هل الدفعة الحالية تكفي لتلبية الطلب لوحدها؟
                if (requestedStripsForThisRow <= currentBatch.getQuantity()) {
                    item.setQuantity(newQuantity);
                    refreshCartTotals();
                } 
                // الحالة الثانية: الدفعة لا تكفي، نحتاج لتوزيع الباقي (Spillover)
                else {
                    // 1. نأخذ أقصى ما يمكن من هذه الدفعة
                    int maxDisplayQtyFromThisBatch = currentBatch.getQuantity() / conversionFactor;
                    item.setQuantity(maxDisplayQtyFromThisBatch);

                    // 2. حساب الكمية المتبقية التي لم يتم تلبيتها بعد
                    int remainingStripsToFulfill = requestedStripsForThisRow - (maxDisplayQtyFromThisBatch * conversionFactor);

                    if (remainingStripsToFulfill > 0) {
                        // جلب باقي الدفعات الصالحة من المستودع
                        List<Batch> validBatches = batchService.getValidBatchesForSale(med.getMed_id());
                        
                        // إزالة الدفعة الحالية من القائمة لأننا استنزفناها للتو
                        validBatches.removeIf(b -> b.getBatch_id() == currentBatch.getBatch_id());

                        int unfulfilledStrips = remainingStripsToFulfill;

                        // المرور على الدفعات لتلبية الباقي
                        for (Batch b : validBatches) {
                            if (unfulfilledStrips <= 0) break;

                            int inCartStrips = getCartQuantityForBatchInStrips(b.getBatch_id(), med.getConversion_factor());
                            int availableStrips = b.getQuantity() - inCartStrips;

                            if (availableStrips > 0) {
                                int stripsToTake = Math.min(unfulfilledStrips, availableStrips);
                                int displayQtyToAdd = stripsToTake / conversionFactor;

                                if (displayQtyToAdd > 0) {
                                    // هل هذا الدواء موجود مسبقاً في السلة من هذه الدفعة؟
                                    CartItem existingOther = findCartItemByBatchAndUnit(b.getBatch_id(), item.getUnit());
                                    if (existingOther != null) {
                                        existingOther.setQuantity(existingOther.getQuantity() + displayQtyToAdd);
                                    } else {
                                        // إضافة سطر جديد للسلة للدفعة الجديدة
                                        CartItem newItem = new CartItem(
                                                med.getBarcode(), med.getBrand_name(),
                                                b.getBatch_id(), b.getBatch_number(),
                                                b.getQuantity(),
                                                item.getUnit(), item.getPrice(),
                                                displayQtyToAdd, BigDecimal.ZERO, med.getPrescription_required() == 1
                                        );
                                        cartItems.add(newItem);
                                    }
                                    unfulfilledStrips -= (displayQtyToAdd * conversionFactor);
                                }
                            }
                        }

                        // إشعار الكاشير بما حدث
                        if (unfulfilledStrips > 0) {
                            showAlert("تنبيه المخزون", "تم أخذ كل المخزون المتاح من جميع الدفعات، ولكنه لا يكفي لتلبية الكمية المطلوبة (" + newQuantity + ") بالكامل.", Alert.AlertType.WARNING);
                        } else {
                            showAlert("توزيع تلقائي (FEFO)", "الدفعة الأقدم لا تكفي، تم توزيع الكمية المطلوبة على عدة دفعات تلقائياً.", Alert.AlertType.INFORMATION);
                        }
                    }
                    // تحديث الواجهة
                    cartTable.refresh();
                    refreshCartTotals();
                }
            }
        });

        // إعداد عمود خصم التأمين ليكون قابلاً للتعديل
        insuranceDiscountCol.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        insuranceDiscountCol.setOnEditCommit(event -> {
            CartItem item = event.getRowValue();
            BigDecimal newDiscount = event.getNewValue();

            // حماية: التأكد من أن الخصم المدخل هو بين 0 و 100
            if (newDiscount != null && newDiscount.compareTo(BigDecimal.ZERO) >= 0 && newDiscount.compareTo(new BigDecimal("100")) <= 0) {
                item.insuranceDiscountProperty().set(newDiscount);
                refreshCartTotals(); 
            } else {
                cartTable.refresh(); 
            }
        });

        // إعداد عمود الوحدة ليكون قائمة منسدلة (ComboBox)
        unitCol.setCellFactory(ComboBoxTableCell.forTableColumn("علبة", "ظرف"));
        unitCol.setOnEditCommit(event -> {
            CartItem item = event.getRowValue();
            String newUnit = event.getNewValue();
            String oldUnit = event.getOldValue();

            Optional<Medicine> optMed = medicineService.getMedicineByBarcode(item.getBarcode());
            Optional<Batch> optBatch = batchService.getBatchById(item.getBatchId());
            
            if (optMed.isPresent() && optBatch.isPresent()) {
                Medicine med = optMed.get();
                Batch b = optBatch.get();

                // التحقق من الرصيد قبل السماح بتغيير الوحدة
                int requestedStrips = newUnit.equals("علبة") ? (item.getQuantity() * med.getConversion_factor()) : item.getQuantity();
                
                // حساب ما تم أخذه من هذه الدفعة في أسطر أخرى
                int inCartOtherRows = getCartQuantityForBatchInStrips(b.getBatch_id(), med.getConversion_factor()) 
                                    - (oldUnit.equals("علبة") ? (item.getQuantity() * med.getConversion_factor()) : item.getQuantity());

                int trueAvailable = b.getQuantity() - inCartOtherRows;

                if (requestedStrips > trueAvailable) {
                    showAlert("خطأ", "لا يوجد رصيد كافي في هذه الدفعة للتحويل إلى علبة.", Alert.AlertType.ERROR);
                    cartTable.refresh(); // إعادة الكلمة القديمة
                    return;
                }

                // إذا كان الرصيد يسمح، قم بالتحديث
                item.unitProperty().set(newUnit);
                if ("علبة".equals(newUnit)) {
                    item.priceProperty().set(med.getCurrent_box_sell_price());
                } else if ("ظرف".equals(newUnit)) {
                    item.priceProperty().set(med.getCurrent_unit_sell_price());
                }
                
                refreshCartTotals(); 
            }
        });

        // 4. إعداد عمود الحذف (Action Column)
        // 4. إعداد عمود الحذف (Action Column) المطور
        // 4. إعداد عمود الحذف (Action Column)
        actionCol.setCellFactory(param -> new TableCell<CartItem, Void>() {
            private final Button deleteBtn = new Button("حذف");

            {
                // 1. إعطاء التصميم المباشر (Inline Style) لضمان تغيير اللون قسرياً
                String normalStyle = "-fx-background-color: #e74c3c; " +
                                     "-fx-text-fill: white; " +
                                     "-fx-font-weight: bold; " +
                                     "-fx-font-size: 12px; " +
                                     "-fx-background-radius: 4px; " +
                                     "-fx-padding: 5;";
                                     
                String hoverStyle =  "-fx-background-color: #c0392b; " + // لون أغمق عند وضع الماوس
                                     "-fx-text-fill: white; " +
                                     "-fx-font-weight: bold; " +
                                     "-fx-font-size: 12px; " +
                                     "-fx-background-radius: 4px; " +
                                     "-fx-padding: 5;";

                deleteBtn.setStyle(normalStyle);
                deleteBtn.setCursor(javafx.scene.Cursor.HAND);

                // تأثير الـ Hover برمجياً
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(hoverStyle));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(normalStyle));

                // 2. ضبط الحجم الدقيق كما في الصورة المرفقة
                deleteBtn.setPrefWidth(55);  
                deleteBtn.setPrefHeight(25);
                deleteBtn.setMinWidth(55);
                deleteBtn.setMaxHeight(25);

                // 3. حدث الحذف
                deleteBtn.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    refreshCartTotals();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * إعداد ميزة الإكمال التلقائي (ControlsFX) لحقول البحث.
     */
    private void setupAutoCompletion() {
        // 1. الإكمال التلقائي للأدوية
        medicineAutoCompletion = TextFields.bindAutoCompletion(searchField, request -> {
            String keyword = request.getUserText().trim();
            if (keyword.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            String searchPattern = "%" + keyword + "%";
            java.util.List<com.pharmacy.models.inventory.Medicine> results = medicineService.searchMedicines(searchPattern);
            
            if (results.isEmpty()) {
                System.out.println("تنبيه النظام: لم يتم العثور على أدوية تطابق: " + keyword);
            }

            return results.stream()
                    .map(med -> med.getBrand_name() + " [" + med.getBarcode() + "]")
                    .collect(Collectors.toList());
        });

        medicineAutoCompletion.setOnAutoCompleted(event -> {
            String selectedText = event.getCompletion();

            // إرسال النص المختار إلى العقل المدبر
            processMedicineInput(selectedText); 

            // مسح الحقل بعد الاختيار
            searchField.clear(); 
        });

        // 2. الإكمال التلقائي للزبائن (الآجل)
        customerAutoCompletion = TextFields.bindAutoCompletion(customerSearchField, request -> {
            String keyword = request.getUserText().trim();
            if (keyword.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            
            String searchPattern = "%" + keyword + "%";

            return customerService.searchCustomers(searchPattern).stream()
                    .map(cust -> cust.getName() + " - " + cust.getPhone())
                    .collect(Collectors.toList());
        });

        // 3. الإكمال التلقائي لشركات التأمين
        insuranceAutoCompletion = TextFields.bindAutoCompletion(insuranceSearchField, request -> {
            String keyword = request.getUserText().trim().toLowerCase();
            if (keyword.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            
            return insuranceService.getAllCompanies().stream()
                    .filter(ins -> ins.getName().toLowerCase().contains(keyword))
                    .map(com.pharmacy.models.pos.InsuranceCompany::getName)
                    .collect(Collectors.toList());
        });
    }

    /**
     * إعداد الـ Bindings التفاعلية لحساب الإجماليات بشكل ديناميكي دون تعقيد الكود.
     */
    private void setupFinancialBindings() {
        totalAmountLabel.textProperty().bind(subTotalProperty.asString("%.2f"));
        totalInsuranceDebtLabel.textProperty().bind(insuranceDebtProperty.asString("%.2f"));
        totalCustomerDebtLabel.textProperty().bind(customerDebtProperty.asString("%.2f"));
        netTotalLabel.textProperty().bind(netTotalProperty.asString("%.2f"));

        // استدعاء دالة الحساب التلقائي عند تغير أي من الإجماليات الأساسية
        subTotalProperty.addListener((obs, oldVal, newVal) -> updateFinancialCalculations());
        discountProperty.addListener((obs, oldVal, newVal) -> updateFinancialCalculations());
        insuranceDebtProperty.addListener((obs, oldVal, newVal) -> updateFinancialCalculations());

        // مراقبة حقل المبلغ المدفوع لحساب الباقي أو الدين
        paidAmountField.textProperty().addListener((obs, oldVal, newVal) -> calculateChangeOrDebt());
        netTotalProperty.addListener((obs, oldVal, newVal) -> calculateChangeOrDebt());
        paymentMethodCombo.valueProperty().addListener((obs, oldVal, newVal) -> calculateChangeOrDebt());
        
        // ربط حقل الخصم النصي بخاصية الخصم (لتحويل النص إلى رقم بأمان)
        if (discountField != null) {
            discountField.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    String text = newVal == null ? "" : newVal.trim();
                    java.math.BigDecimal discount = text.isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(text);
                    discountProperty.set(discount);
                } catch (NumberFormatException e) {
                    discountProperty.set(java.math.BigDecimal.ZERO);
                }
            });
        }
    }

    /**
     * إعداد مستمعات التغير (Listeners) لحقول الإدخال وقوائم الاختيار.
     */
    private void setupListeners() {
        // 1. تعبئة قائمة طرق الدفع
        paymentMethodCombo.getItems().addAll("نقدي", "ذمة مالية");
        paymentMethodCombo.getSelectionModel().selectFirst(); // اختيار "نقدي" كافتراضي

        // 2. مراقبة تغيير طريقة الدفع لإظهار/إخفاء حقل الزبون
        paymentMethodCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            handlePaymentMethodChange(newValue);
        });
        
        // 3. تحسين تجربة المستخدم: تحديد النص بالكامل عند النقر على حقل الخصم
        if (discountField != null) {
            discountField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) { // عند دخول المستخدم إلى الحقل
                    javafx.application.Platform.runLater(() -> {
                        if ("0.00".equals(discountField.getText()) || "0".equals(discountField.getText())) {
                            discountField.clear(); 
                        } else {
                            discountField.selectAll(); 
                        }
                    });
                } else { // عند خروج المستخدم من الحقل
                    if (discountField.getText() == null || discountField.getText().trim().isEmpty()) {
                        discountField.setText("0.00"); 
                    }
                }
            });
        }
    }

   // ==========================================
    // 5. تفاعلات المستخدم (Event Handlers)
    // ==========================================
    
    /**
     * إضافة دواء للسلة بشكل يدوي (نافذة منبثقة أو آلية بديلة).
     */
    @FXML
    private void handleManualAdd(javafx.event.ActionEvent event) {
        // سيتم برمجة نافذة الإضافة اليدوية لاحقاً
        System.out.println("زر الإضافة اليدوية قيد التطوير...");
    }
    /**
     * معالجة إدخال الباركود أو اختيار دواء من قائمة البحث.
     */
    // هذه الدالة ترتبط بملف الـ FXML وتعمل عند ضرب الباركود أو ضغط Enter في الحقل
    @FXML
    private void handleBarcodeScan(ActionEvent event) { 
        processMedicineInput(searchField.getText());
    }

    // هذه الدالة هي "العقل المدبر" الذي يعالج النص القادم سواء من السكانر أو الـ AutoComplete
    private void processMedicineInput(String input) {
        if (input == null || input.trim().isEmpty()) return;

        String query = input.trim();
        Optional<Medicine> optMedicine = Optional.empty();

        // 1. الفلترة الذكية (هل هو باركود أم اسم؟)
        if (query.matches("\\d+")) {
            // حالة مسح الباركود مباشرة
            optMedicine = medicineService.getMedicineByBarcode(query);
        } 
        else if (query.contains("[") && query.contains("]")) {
            // حالة اختيار دواء من الـ AutoComplete 
            String extractedBarcode = query.substring(query.lastIndexOf("[") + 1, query.lastIndexOf("]")).trim();
            optMedicine = medicineService.getMedicineByBarcode(extractedBarcode);
        }

        // 2. التعامل مع النتيجة
        if (optMedicine.isPresent()) {
            addMedicineToCart(optMedicine.get()); // استدعاء خوارزمية التوزيع

            searchField.clear();
            searchField.requestFocus(); 
        } else {
            List<Medicine> searchResults = medicineService.searchMedicines(query);
            if (searchResults.size() == 1) {
                addMedicineToCart(searchResults.get(0));
                searchField.clear();
            } else if (searchResults.size() > 1) {
                showAlert("تنبيه", "يوجد أكثر من دواء بهذا الاسم. يرجى اختياره من القائمة بدقة.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("غير موجود", "لم يتم العثور على الدواء: " + query, Alert.AlertType.WARNING);
                searchField.selectAll(); 
            }
        }
    }

// دالة الإضافة التي تحتوي على خوارزمية التوزيع (Spillover) التي أصلحناها سابقاً
private void addMedicineToCart(Medicine medicine) {
    // 1. جلب الدفعات الصالحة (مرتبة من الأقدم للأحدث FEFO)
    List<Batch> validBatches = batchService.getValidBatchesForSale(medicine.getMed_id());
    
    int totalAvailableStrips = 0;
    for (Batch b : validBatches) {
        int inCart = getCartQuantityForBatchInStrips(b.getBatch_id(), medicine.getConversion_factor());
        totalAvailableStrips += (b.getQuantity() - inCart);
    }

    int requestedStrips = medicine.getConversion_factor(); // افتراضياً يريد علبة كاملة
    String defaultUnit = "علبة";
    BigDecimal defaultPrice = medicine.getCurrent_box_sell_price();

    if (totalAvailableStrips < requestedStrips) {
        if (totalAvailableStrips > 0) {
            requestedStrips = 1; // نعطيه ظرف واحد افتراضياً بدلاً من العلبة الناقصة
            defaultUnit = "ظرف";
            defaultPrice = medicine.getCurrent_unit_sell_price();
        } else {
            showAlert("نقص مخزون", "لا يوجد رصيد كافي من الدواء: " + medicine.getBrand_name(), Alert.AlertType.ERROR);
            return;
        }
    }

    int remainingStripsToFulfill = requestedStrips;
    boolean willSplitAcrossBatches = false;
    for (Batch batch : validBatches) {
        int inCart = getCartQuantityForBatchInStrips(batch.getBatch_id(), medicine.getConversion_factor());
        int availableInThisBatch = batch.getQuantity() - inCart;
        if (availableInThisBatch > 0 && availableInThisBatch < remainingStripsToFulfill) {
            willSplitAcrossBatches = true;
            break;
        }
    }

    if (willSplitAcrossBatches && defaultUnit.equals("علبة")) {
        defaultUnit = "ظرف";
        defaultPrice = medicine.getCurrent_unit_sell_price();
    }

    for (Batch batch : validBatches) {
        if (remainingStripsToFulfill <= 0) break;

        int inCart = getCartQuantityForBatchInStrips(batch.getBatch_id(), medicine.getConversion_factor());
        int availableInThisBatch = batch.getQuantity() - inCart;

        if (availableInThisBatch > 0) {
            int stripsToTakeFromThisBatch = Math.min(remainingStripsToFulfill, availableInThisBatch);
            int displayQuantityToAdd = defaultUnit.equals("علبة") ? 
                    (stripsToTakeFromThisBatch / medicine.getConversion_factor()) : stripsToTakeFromThisBatch;

            CartItem existingItem = findCartItemByBatchAndUnit(batch.getBatch_id(), defaultUnit);
            
            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + displayQuantityToAdd);
            } else {
                CartItem newItem = new CartItem(
                        medicine.getBarcode(), medicine.getBrand_name(),
                        batch.getBatch_id(), batch.getBatch_number(),
                        batch.getQuantity(), 
                        defaultUnit, defaultPrice,
                        displayQuantityToAdd, BigDecimal.ZERO, medicine.getPrescription_required() == 1
                );
                cartItems.add(newItem);
            }
            remainingStripsToFulfill -= stripsToTakeFromThisBatch;
        }
    }
    refreshCartTotals();
}

    // --- دوال مساعدة للخوارزمية ---
    private int getCartQuantityForBatchInStrips(int batchId, int unitsPerBox) {
        int totalStrips = 0;
        for (CartItem item : cartItems) {
            if (item.getBatchId() == batchId) {
                int multiplier = item.getUnit().equals("علبة") ? unitsPerBox : 1;
                totalStrips += (item.getQuantity() * multiplier);
            }
        }
        return totalStrips;
    }

    private CartItem findCartItemByBatchAndUnit(int batchId, String unit) {
        for (CartItem item : cartItems) {
            if (item.getBatchId() == batchId && item.getUnit().equals(unit)) return item;
        }
        return null;
    }
    
    /**
     * معالجة تغيير طريقة الدفع لإظهار أو إخفاء الحقول المرتبطة بها.
     */
    private void handlePaymentMethodChange(String newValue) {
        boolean isCredit = "ذمة مالية".equals(newValue);
        
        customerSearchField.setVisible(isCredit);
        customerSearchField.setManaged(isCredit);
        
        paidAmountField.setVisible(isCredit);
        paidAmountField.setManaged(isCredit);
        
        if (!isCredit) {
            customerSearchField.clear();
            paidAmountField.clear(); 
        }

        calculateChangeOrDebt();
    }
    
    /**
     * فتح نافذة لاختيار صورة الوصفة الطبية من الجهاز.
     */
   
    @FXML
    private void handleAttachPrescription(javafx.event.ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("اختر صورة الوصفة الطبية");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.pdf")
        );
        
        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null) {
            currentPrescriptionImagePath = selectedFile.getAbsolutePath();
            attachPrescriptionButton.setText("تم الإرفاق: " + selectedFile.getName());
            attachPrescriptionButton.getStyleClass().add("button-success"); 
        }
    }
    
    /**
     * تنفيذ عملية الدفع وحفظ الفاتورة في قاعدة البيانات.
     */
    @FXML
    private void handleCheckout(javafx.event.ActionEvent event) {
        // 1. التحقق من صحة المدخلات
        if (!validateCheckout()) {
            return;
        }
        
        // 2. التحقق من الوصفات الطبية الإجبارية
        boolean needsPrescription = cartItems.stream().anyMatch(CartItem::isPrescriptionRequired);
        
        if (needsPrescription) {
            boolean success = promptForPrescriptionDetails();
            if (!success) {
                return; // إيقاف العملية إذا ألغى الكاشير نافذة الوصفة
            }
        } else {
            this.currentDoctorName = null;
            this.currentPatientName = null;
            this.currentPrescriptionImagePath = null;
        }
        
        // 3. بناء وتجهيز كائن الفاتورة (Sale)
        try {
            com.pharmacy.models.pos.Sale sale = new com.pharmacy.models.pos.Sale();
            
            sale.setShift_id(com.pharmacy.security.SessionManager.getInstance().getCurrentShift().getShift_id());
            sale.setSale_date(java.time.LocalDateTime.now());
            sale.setPayment_method(paymentMethodCombo.getValue());
            sale.setStatus("Completed");
            
            // البيانات المالية
            sale.setTotal_amount(subTotalProperty.get()); 
            sale.setDiscount_amount(discountProperty.get());
            sale.setRounding_adjustment(roundingProperty.get());
            
            java.math.BigDecimal net = netTotalProperty.get() != null ? netTotalProperty.get() : java.math.BigDecimal.ZERO;
            String paidText = paidAmountField.getText() != null ? paidAmountField.getText().trim() : "";
            java.math.BigDecimal paid;
            
            if (paidText.isEmpty() && !"ذمة مالية".equals(paymentMethodCombo.getValue())) {
                paid = net; 
            } else {
                paid = paidText.isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(paidText);
            }
            
            sale.setTotal_patient_paid(paid);
            sale.setTotal_customer_debt(customerDebtProperty.get());
            sale.setTotal_insurance_debt(insuranceDebtProperty.get());
            
            // بيانات الوصفة
            sale.setDoctor_name(currentDoctorName);
            sale.setPatient_name(currentPatientName);
            sale.setPrescription_image_path(currentPrescriptionImagePath);

            // ربط الزبون (للذمم المالية)
            if ("ذمة مالية".equals(paymentMethodCombo.getValue())) {
                String customerText = customerSearchField.getText();
                String customerName = customerText.contains("-") ? customerText.split("-")[0].trim() : customerText.trim();
                java.util.List<com.pharmacy.models.pos.LocalCustomer> customers = customerService.searchCustomers(customerName);
                if (!customers.isEmpty()) {
                    sale.setCustomer_id(customers.get(0).getCustomer_id());
                }
            }

            // ربط شركة التأمين
            if (insuranceDebtProperty.get().compareTo(java.math.BigDecimal.ZERO) > 0) {
                String insuranceName = insuranceSearchField.getText().trim();
                java.util.Optional<com.pharmacy.models.pos.InsuranceCompany> optIns = insuranceService.getCompanyByName(insuranceName);
                optIns.ifPresent(ins -> sale.setInsurance_id(ins.getInsurance_id()));
                sale.setInsurance_approval_code(insuranceApprovalField.getText().trim());
            }

            // 4. الحفظ في قاعدة البيانات عبر الـ Services
            java.util.Optional<com.pharmacy.models.pos.Sale> createdSale = saleService.createSale(sale);
            
            if (createdSale.isPresent()) {
                int saleId = createdSale.get().getSale_id();
                
                // حفظ تفاصيل كل دواء مباع وتحديث المستودع
                for (CartItem item : cartItems) {
                    com.pharmacy.models.pos.SaleDetail detail = new com.pharmacy.models.pos.SaleDetail();
                    detail.setSale_id(saleId);
                    detail.setBatch_id(item.getBatchId());
                    detail.setQuantity_sold(item.getQuantity());
                    detail.setUnit_sell_price(item.getPrice());
                    
                    java.math.BigDecimal itemTotal = item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()));
                    java.math.BigDecimal insDiscount = item.getInsuranceDiscount() != null ? item.getInsuranceDiscount() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal insShare = itemTotal.multiply(insDiscount).divide(java.math.BigDecimal.valueOf(100));
                    java.math.BigDecimal patShare = itemTotal.subtract(insShare);

                    detail.setPatient_share(patShare);
                    detail.setInsurance_share(insShare);
                    detail.setSubtotal(itemTotal); 
                    
                    saleService.addSaleDetail(detail);
                    
                    // الخصم اللحظي من الدفعة (Batch)
                    Optional<Medicine> optMed = medicineService.getMedicineByBarcode(item.getBarcode());
                    Optional<Batch> optBatch = batchService.getBatchById(item.getBatchId());
                    
                    if (optMed.isPresent() && optBatch.isPresent()) {
                        Medicine med = optMed.get();
                        Batch b = optBatch.get();
                        
                        // معامل التحويل: إذا كانت الوحدة علبة، نضرب في عدد ظروف العلبة، وإلا نضرب في 1
                        int conversionFactor = item.getUnit().equals("علبة") ? med.getConversion_factor(): 1;
                        int totalStripsToDeduct = item.getQuantity() * conversionFactor;
                        
                        b.setQuantity(b.getQuantity() - totalStripsToDeduct);
                        batchService.updateBatch(b);
                    }
                }
                
                showAlert("عملية ناجحة", "تم حفظ وإصدار الفاتورة بنجاح. رقم الفاتورة: " + saleId, javafx.scene.control.Alert.AlertType.INFORMATION);
                handleClearCart(null); 
                
            } else {
                showAlert("خطأ", "فشل حفظ الفاتورة في قاعدة البيانات. تواصل مع مدير النظام.", javafx.scene.control.Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            showAlert("خطأ نظام غير متوقع", "حدث خطأ أثناء حفظ الفاتورة: " + e.getMessage(), javafx.scene.control.Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    /**
     * تفريغ السلة وإعادة ضبط الواجهة لاستقبال زبون جديد.
     */
    @FXML
    private void handleClearCart(javafx.event.ActionEvent event) {
        if (cartItems != null) {
            cartItems.clear();
        }

        subTotalProperty.set(java.math.BigDecimal.ZERO);
        discountProperty.set(java.math.BigDecimal.ZERO);
        roundingProperty.set(java.math.BigDecimal.ZERO);
        customerDebtProperty.set(java.math.BigDecimal.ZERO);
        insuranceDebtProperty.set(java.math.BigDecimal.ZERO);

        if (discountField != null) discountField.setText("0.00");
        if (paidAmountField != null) paidAmountField.clear();
        if (customerSearchField != null) customerSearchField.clear();
        if (insuranceSearchField != null) insuranceSearchField.clear();
        if (insuranceApprovalField != null) insuranceApprovalField.clear();
        if (searchField != null) searchField.clear();

        this.currentDoctorName = null;
        this.currentPatientName = null;
        this.currentPrescriptionImagePath = null;
        
        if (attachPrescriptionButton != null) {
            attachPrescriptionButton.setText("إرفاق وصفة طبية");
            attachPrescriptionButton.getStyleClass().remove("button-success");
        }

        checkInsuranceVisibility();
        
        if (searchField != null) {
            javafx.application.Platform.runLater(searchField::requestFocus);
        }
    }
    
    
    
    // ==========================================
    // 6. العمليات الحسابية والمنطق المالي (Business Logic)
    // ==========================================
    
    /**
     * إعادة حساب إجماليات السلة (الإجمالي، الخصومات، ذمة التأمين).
     */
    private void refreshCartTotals() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal insuranceTotal = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);

            // حساب حصة التأمين لهذا الدواء بناءً على النسبة المئوية
            BigDecimal itemInsDiscount = item.getInsuranceDiscount() != null ? item.getInsuranceDiscount() : BigDecimal.ZERO;
            BigDecimal insShare = itemTotal.multiply(itemInsDiscount).divide(BigDecimal.valueOf(100));
            insuranceTotal = insuranceTotal.add(insShare);
            
            // الصافي الخاص بالسطر الواحد (بعد خصم حصة التأمين)
            item.subTotalProperty().set(itemTotal.subtract(insShare)); 
        }
        
        subTotalProperty.set(total);
        insuranceDebtProperty.set(insuranceTotal);
        checkInsuranceVisibility();
    }
    
    /**
     * حساب الإجماليات النهائية وتطبيق التسوية الجبرية التلقائية.
     */
    private void updateFinancialCalculations() {
        BigDecimal subTotal = subTotalProperty.get() != null ? subTotalProperty.get() : BigDecimal.ZERO;
        BigDecimal discount = discountProperty.get() != null ? discountProperty.get() : BigDecimal.ZERO;
        BigDecimal insurance = insuranceDebtProperty.get() != null ? insuranceDebtProperty.get() : BigDecimal.ZERO;

        // 1. حساب الصافي الأولي قبل التسوية
        BigDecimal initialNet = subTotal.subtract(discount).subtract(insurance);
        if (initialNet.compareTo(BigDecimal.ZERO) < 0) {
            initialNet = BigDecimal.ZERO;
        }

        // 2. تطبيق قاعدة التسوية الجبرية: التقريب للأعلى لأقرب 5 ليرات
        double initialDouble = initialNet.doubleValue();
        double roundedDouble = Math.ceil(initialDouble / 5.0) * 5.0;
        
        BigDecimal finalNet = BigDecimal.valueOf(roundedDouble);
        
        // 3. حساب قيمة الحمل (التسوية)
        BigDecimal roundingValue = finalNet.subtract(initialNet);

        // 4. تحديث المتغيرات المرتبطة بالواجهة وقاعدة البيانات
        roundingProperty.set(roundingValue);
        netTotalProperty.set(finalNet);
    }
    
    /**
     * حساب ذمة الزبون أو التحقق من المدفوع نقدياً.
     */
    private void calculateChangeOrDebt() {
        try {
            BigDecimal net = netTotalProperty.get() != null ? netTotalProperty.get() : BigDecimal.ZERO;
            boolean isCredit = "ذمة مالية".equals(paymentMethodCombo.getValue());

            // 1. الدفع النقدي: نعتبر أن الفاتورة مدفوعة بالكامل ولا يوجد دين
            if (!isCredit) {
                changeTitleLabel.setText("حالة الدفع:");
                changeAmountLabel.setText("مدفوعة بالكامل (نقدي)");
                customerDebtProperty.set(BigDecimal.ZERO);
                return;
            }

            // 2. الدفع الآجل (ذمة مالية)
            String paidText = paidAmountField.getText().trim();
            BigDecimal paid = paidText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(paidText);

            // حماية: منع إدخال مبلغ دفع أكبر من الصافي المطلوب
            if (paid.compareTo(net) > 0) {
                paid = net; 
                final String maxValStr = net.toString();
                javafx.application.Platform.runLater(() -> {
                    paidAmountField.setText(maxValStr);
                    paidAmountField.positionCaret(maxValStr.length()); 
                });
            }

            // حساب الدين المتبقي
            BigDecimal debt = net.subtract(paid);
            changeTitleLabel.setText("الدين المسجل (آجل):");
            changeAmountLabel.setText(debt.toString());
            customerDebtProperty.set(debt); 

        } catch (NumberFormatException e) {
            changeAmountLabel.setText("إدخال غير صالح");
        }
    }
    
    /**
     * فحص السلة لإظهار أو إخفاء حقول التأمين.
     */
    private void checkInsuranceVisibility() {
        boolean hasInsurance = cartItems.stream()
                .anyMatch(item -> item.getInsuranceDiscount() != null && 
                                  item.getInsuranceDiscount().compareTo(BigDecimal.ZERO) > 0);

        insuranceSearchField.setVisible(hasInsurance);
        insuranceSearchField.setManaged(hasInsurance);
        
        insuranceApprovalField.setVisible(hasInsurance);
        insuranceApprovalField.setManaged(hasInsurance);
        
        attachPrescriptionButton.setVisible(hasInsurance);
        attachPrescriptionButton.setManaged(hasInsurance);

        if (!hasInsurance) {
            insuranceSearchField.clear();
            insuranceApprovalField.clear();
        }
    }
    
    
    
    // ==========================================
    // 7. التحقق والنوافذ المنبثقة (Validation & Dialogs)
    // ==========================================

    /**
     * التحقق الصارم من صحة بيانات الفاتورة قبل الحفظ.
     * @return true إذا كانت البيانات صحيحة وجاهزة.
     */
    /**
     * التحقق الصارم من صحة بيانات الفاتورة قبل الحفظ.
     * @return true إذا كانت البيانات صحيحة وجاهزة.
     */
    private boolean validateCheckout() {
        // 1. التأكد من وجود أدوية في السلة
        if (cartItems.isEmpty()) {
            showAlert("تنبيه", "السلة فارغة. يرجى إضافة أدوية قبل محاولة الحفظ.", Alert.AlertType.WARNING);
            return false;
        }

        // 2. التأكد من وجود وردية (Shift) مفتوحة
        if (com.pharmacy.security.SessionManager.getInstance().getCurrentShift() == null) {
            showAlert("خطأ أمني", "لا يوجد وردية مفتوحة حالياً للقيام بعمليات البيع.", Alert.AlertType.ERROR);
            return false;
        }

        // 3. التحقق من طريقة الدفع
        String paymentMethod = paymentMethodCombo.getValue();
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            showAlert("تنبيه", "يرجى تحديد طريقة الدفع أولاً.", Alert.AlertType.WARNING);
            return false;
        }

        // 4. التحقق من الزبون في حال كانت الفاتورة ذمة مالية
        if ("ذمة مالية".equals(paymentMethod)) {
            if (customerSearchField.getText() == null || customerSearchField.getText().trim().isEmpty()) {
                showAlert("بيانات ناقصة", "بما أن طريقة الدفع ذمة مالية، يجب اختيار اسم الزبون.", Alert.AlertType.WARNING);
                return false;
            }
        } else {
            // 5. التحقق المالي الذكي للفواتير النقدية
            BigDecimal net = netTotalProperty.get() != null ? netTotalProperty.get() : BigDecimal.ZERO;
            String paidText = paidAmountField.getText() != null ? paidAmountField.getText().trim() : "";
            BigDecimal paid;

            if (paidText.isEmpty()) {
                // إذا ترك الحقل فارغاً، نفترض أنه استلم المبلغ بالكامل
                paid = net;
            } else {
                try {
                    paid = new BigDecimal(paidText);
                } catch (NumberFormatException e) {
                    showAlert("إدخال خاطئ", "يرجى التأكد من كتابة المبلغ المدفوع بصيغة أرقام صحيحة.", Alert.AlertType.ERROR);
                    return false;
                }
            }
            
            if (paid.compareTo(net) < 0) {
                showAlert("نقص في الدفع", "المبلغ المدفوع نقدًا لا يغطي الصافي المطلوب من الزبون.", Alert.AlertType.ERROR);
                return false;
            }
        }

        // 6. التحقق من شركة التأمين (في حال وجود خصم تأميني في السلة)
        BigDecimal insDebt = insuranceDebtProperty.get() != null ? insuranceDebtProperty.get() : BigDecimal.ZERO;
        if (insDebt.compareTo(BigDecimal.ZERO) > 0) {
            String insuranceName = insuranceSearchField.getText();
            if (insuranceName == null || insuranceName.trim().isEmpty()) {
                showAlert("بيانات التأمين ناقصة", "الفاتورة تحتوي على خصم تأميني. يرجى اختيار اسم شركة التأمين لإتمام العملية.", Alert.AlertType.WARNING);
                
                // إعادة التركيز على حقل شركة التأمين لتنبيه الكاشير
                javafx.application.Platform.runLater(insuranceSearchField::requestFocus);
                return false;
            }
        }

        return true;
    }
    
    /**
     * إظهار رسائل التنبيه للمستخدم.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * تعرض نافذة منبثقة إجبارية لجمع بيانات الوصفة الطبية.
     * @return true إذا قام الكاشير بإدخال البيانات، false إذا ألغى العملية.
     */
    private boolean promptForPrescriptionDetails() {
        // 1. إنشاء النافذة المنبثقة
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("معلومات الوصفة الطبية");
        dialog.setHeaderText("تحتوي السلة على أدوية تتطلب وصفة طبية إجبارية.\nالرجاء إدخال بيانات الوصفة لإتمام الفاتورة.");

        // 2. إعداد أزرار النافذة (متابعة / إلغاء)
        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("متابعة وحفظ", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        // 3. بناء واجهة المستخدم داخل النافذة
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        javafx.scene.control.TextField docNameField = new javafx.scene.control.TextField();
        docNameField.setPromptText("اسم الطبيب المعالج...");
        
        javafx.scene.control.TextField patNameField = new javafx.scene.control.TextField();
        patNameField.setPromptText("اسم المريض المستفيد...");
        
        javafx.scene.control.Button attachBtn = new javafx.scene.control.Button("إرفاق صورة الوصفة (اختياري)");
        javafx.scene.control.Label attachLabel = new javafx.scene.control.Label("لم يتم إرفاق صورة");
        attachLabel.setTextFill(javafx.scene.paint.Color.GRAY);

        // برمجة زر إرفاق الصورة
        attachBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("اختر صورة الوصفة الطبية");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
            if (selectedFile != null) {
                currentPrescriptionImagePath = selectedFile.getAbsolutePath();
                attachLabel.setText("تم الإرفاق: " + selectedFile.getName());
                attachLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            }
        });

        // ترتيب العناصر في الشبكة
        grid.add(new javafx.scene.control.Label("اسم الطبيب:"), 0, 0);
        grid.add(docNameField, 1, 0);
        grid.add(new javafx.scene.control.Label("اسم المريض:"), 0, 1);
        grid.add(patNameField, 1, 1);
        grid.add(attachBtn, 0, 2);
        grid.add(attachLabel, 1, 2);

        // 4. الحماية الأمنية (تعطيل زر المتابعة إذا كانت الحقول فارغة)
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        javafx.beans.value.ChangeListener<String> validationListener = (obs, oldVal, newVal) -> {
            boolean isInvalid = docNameField.getText().trim().isEmpty() || patNameField.getText().trim().isEmpty();
            saveButton.setDisable(isInvalid);
        };
        docNameField.textProperty().addListener(validationListener);
        patNameField.textProperty().addListener(validationListener);

        dialog.getDialogPane().setContent(grid);
        
        // 5. التركيز التلقائي على حقل اسم الطبيب لسرعة الكتابة
        javafx.application.Platform.runLater(docNameField::requestFocus);

        // 6. عرض النافذة وانتظار رد المستخدم
        java.util.Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            this.currentDoctorName = docNameField.getText().trim();
            this.currentPatientName = patNameField.getText().trim();
            return true;
        }
        
        return false;
    }
}
    
   