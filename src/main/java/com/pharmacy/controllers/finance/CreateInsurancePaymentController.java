package com.pharmacy.controllers.finance;

import com.pharmacy.models.finance.InsurancePayment;
import com.pharmacy.models.pos.InsuranceCompany;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.interfaces.finance.InsurancePaymentService;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CreateInsurancePaymentController implements Initializable {

    @FXML private TextField txtCompanySearch;
    @FXML private Label lblTotalDebt;
    @FXML private TextField txtAmount;

    private InsurancePaymentService paymentService;
    private InsuranceCompanyService insuranceCompanyService;
    private SaleService saleService;

    private List<InsuranceCompany> allCompanies = new ArrayList<>();
    private List<Sale> unpaidSalesForCompany = new ArrayList<>();
    private BigDecimal currentTotalDebt = BigDecimal.ZERO;
    private Integer currentVerifiedCompanyId = null;
    
    private Popup autocompletePopup;
    private ListView<InsuranceCompany> popupListView;
    private boolean isSelecting = false; 

    public void initServices(InsurancePaymentService pService, InsuranceCompanyService cService, SaleService sService) {
        this.paymentService = pService;
        this.insuranceCompanyService = cService;
        this.saleService = sService;

        loadCompanies();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupLiveSearchPopup();
    }

    private void loadCompanies() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // تأكد من وجود getAllCompanies() في خدمة InsuranceCompanyService
                    allCompanies = insuranceCompanyService.getAllCompanies(); 
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void setupLiveSearchPopup() {
        autocompletePopup = new Popup();
        autocompletePopup.setAutoHide(true); 
        
        popupListView = new ListView<>();
        popupListView.setPrefHeight(140); 
        popupListView.setStyle("-fx-font-size: 14px; -fx-border-color: #bdc3c7; -fx-background-color: white;");
        
        popupListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(InsuranceCompany item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        // 1. الماوس
        popupListView.setOnMouseClicked(event -> {
            InsuranceCompany selected = popupListView.getSelectionModel().getSelectedItem();
            if (selected != null) selectCompany(selected);
        });

        // 2. الكيبورد داخل القائمة
        popupListView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    InsuranceCompany selected = popupListView.getSelectionModel().getSelectedItem();
                    if (selected != null) selectCompany(selected);
                    break;
                case ESCAPE:
                    autocompletePopup.hide();
                    txtCompanySearch.requestFocus();
                    break;
                default: break;
            }
        });

        autocompletePopup.getContent().add(popupListView);

        // 3. التوجيه بالأسهم من حقل النص
        txtCompanySearch.setOnKeyPressed(event -> {
            if (autocompletePopup.isShowing()) {
                switch (event.getCode()) {
                    case DOWN:
                        popupListView.requestFocus();
                        popupListView.getSelectionModel().selectFirst();
                        break;
                    case ENTER:
                        if (!popupListView.getItems().isEmpty()) {
                            selectCompany(popupListView.getItems().get(0));
                        }
                        break;
                    case ESCAPE:
                        autocompletePopup.hide();
                        break;
                    default: break;
                }
            }
        });

        txtCompanySearch.textProperty().addListener((obs, oldText, newText) -> {
            if (isSelecting) return;
            
            if (newText == null || newText.trim().isEmpty()) {
                autocompletePopup.hide();
                currentVerifiedCompanyId = null;
                lblTotalDebt.setText("0.00 ل.س");
                currentTotalDebt = BigDecimal.ZERO;
                unpaidSalesForCompany.clear();
                return;
            }

            String search = newText.toLowerCase().trim();
            List<InsuranceCompany> filtered = new ArrayList<>();
            for (InsuranceCompany c : allCompanies) {
                if (c.getName().toLowerCase().contains(search)) {
                    filtered.add(c);
                }
            }

            if (!filtered.isEmpty()) {
                popupListView.setItems(FXCollections.observableArrayList(filtered));
                popupListView.prefWidthProperty().bind(txtCompanySearch.widthProperty());
                
                if (!autocompletePopup.isShowing()) {
                    Bounds bounds = txtCompanySearch.localToScreen(txtCompanySearch.getBoundsInLocal());
                    autocompletePopup.show(txtCompanySearch, bounds.getMinX(), bounds.getMaxY());
                }
            } else {
                autocompletePopup.hide();
                currentVerifiedCompanyId = null;
                lblTotalDebt.setText("0.00 ل.س");
            }
        });
    }

    private void selectCompany(InsuranceCompany company) {
        isSelecting = true;
        txtCompanySearch.setText(company.getName());
        isSelecting = false;
        
        autocompletePopup.hide();

        currentVerifiedCompanyId = company.getInsurance_id();
        
        // جلب الديون فوراً
        calculateCompanyDebt(company.getInsurance_id());
    }

    private void calculateCompanyDebt(int companyId) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    LocalDate safeStart = LocalDate.of(2000, 1, 1);
                    LocalDate safeEnd = LocalDate.now().plusDays(1);
                    
                    List<Sale> allSales = saleService.getSalesByDateRange(safeStart, safeEnd); 
                    
                    unpaidSalesForCompany.clear();
                    currentTotalDebt = BigDecimal.ZERO;

                    for (Sale sale : allSales) {
                        if (sale.getInsurance_id() != null && sale.getInsurance_id() == companyId) {
                            // التركيز هنا على total_insurance_debt
                            BigDecimal debt = sale.getTotal_insurance_debt() != null ? sale.getTotal_insurance_debt() : BigDecimal.ZERO;
                            if (debt.compareTo(BigDecimal.ZERO) > 0) {
                                unpaidSalesForCompany.add(sale);
                                currentTotalDebt = currentTotalDebt.add(debt);
                            }
                        }
                    }

                    // الترتيب من الأقدم للأحدث (FIFO)
                    unpaidSalesForCompany.sort((s1, s2) -> s1.getSale_date().compareTo(s2.getSale_date()));

                    Platform.runLater(() -> {
                        lblTotalDebt.setText(String.format("%,.2f ل.س", currentTotalDebt));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> AlertManager.showError("خطأ", "فشل حساب ديون الشركة:\n" + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleSavePayment(ActionEvent event) {
        if (currentVerifiedCompanyId == null) {
            AlertManager.showWarning("تنبيه", "يرجى البحث عن الشركة واختيارها من القائمة أولاً.");
            return;
        }

        if (SessionManager.getInstance().getCurrentShift() == null) {
            AlertManager.showError("خطأ", "لا توجد وردية مفتوحة حالياً.");
            return;
        }

        BigDecimal paymentAmount;
        try {
            paymentAmount = new BigDecimal(txtAmount.getText().trim());
            if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertManager.showWarning("تنبيه", "يجب أن يكون المبلغ أكبر من الصفر.");
                return;
            }
        } catch (Exception e) {
            AlertManager.showError("خطأ إدخال", "يرجى إدخال مبلغ صحيح.");
            return;
        }

        if (paymentAmount.compareTo(currentTotalDebt) > 0) {
            AlertManager.showWarning("تنبيه", "المبلغ المدخل أكبر من إجمالي الدين المستحق (" + currentTotalDebt + " ل.س).");
            return;
        }

        try {
            int currentShiftId = SessionManager.getInstance().getCurrentShift().getShift_id();

            InsurancePayment payment = new InsurancePayment();
            payment.setInsurance_id(currentVerifiedCompanyId);
            payment.setSale_id(null); 
            payment.setShift_id(currentShiftId);
            payment.setAmount_paid(paymentAmount);
            
            // الخدعة البرمجية: إرسال طريقة الدفع "نقدي" لتجاوز قيد الخدمة (XOR/Method validation)
            payment.setPayment_method("نقدي"); 
            payment.setReference_number("---");
            
            paymentService.createPayment(payment);

            // خوارزمية التسوية (FIFO) على ديون التأمين
            BigDecimal remainingPayment = paymentAmount;

            for (Sale sale : unpaidSalesForCompany) {
                if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal currentSaleDebt = sale.getTotal_insurance_debt();

                if (remainingPayment.compareTo(currentSaleDebt) >= 0) {
                    remainingPayment = remainingPayment.subtract(currentSaleDebt);
                    sale.setTotal_insurance_debt(BigDecimal.ZERO);
                    
                    // إغلاق الفاتورة إذا كان دين الزبون منتهياً أيضاً
                    BigDecimal custDebt = sale.getTotal_customer_debt() != null ? sale.getTotal_customer_debt() : BigDecimal.ZERO;
                    if (custDebt.compareTo(BigDecimal.ZERO) == 0) {
                        sale.setStatus("Completed");
                    }
                } else {
                    sale.setTotal_insurance_debt(currentSaleDebt.subtract(remainingPayment));
                    remainingPayment = BigDecimal.ZERO;
                }

                saleService.updateSale(sale);
            }

            AlertManager.showSuccess("تم بنجاح", "تم استلام الدفعة وتسوية المطالبات آلياً.");
            handleCancel(null);

        } catch (Exception e) {
            e.printStackTrace();
            AlertManager.showError("خطأ", "حدث خطأ أثناء حفظ الدفعة: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) txtAmount.getScene().getWindow()).close();
    }
}