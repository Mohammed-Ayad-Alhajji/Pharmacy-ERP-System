package com.pharmacy.controllers.finance;

import com.pharmacy.models.finance.CustomerPayment;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.interfaces.finance.CustomerPaymentService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CreateCustomerPaymentController implements Initializable {

    @FXML private TextField txtCustomerSearch;
    @FXML private Label lblTotalDebt;
    @FXML private TextField txtAmount;

    private CustomerPaymentService paymentService;
    private LocalCustomerService customerService;
    private SaleService saleService;

    private List<LocalCustomer> allCustomers = new ArrayList<>();
    private List<Sale> unpaidSalesForCustomer = new ArrayList<>();
    private BigDecimal currentTotalDebt = BigDecimal.ZERO;
    private Integer currentVerifiedCustomerId = null;
    
    // عناصر القائمة العائمة (Popup)
    private Popup autocompletePopup;
    private ListView<LocalCustomer> popupListView;
    private boolean isSelecting = false; 

    public void initServices(CustomerPaymentService pService, LocalCustomerService cService, SaleService sService) {
        this.paymentService = pService;
        this.customerService = cService;
        this.saleService = sService;

        loadCustomers();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupLiveSearchPopup();
    }

    private void loadCustomers() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    allCustomers = customerService.getAllCustomers(); 
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
            protected void updateItem(LocalCustomer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + (item.getPhone() != null ? " - " + item.getPhone() : ""));
                }
            }
        });

        // 1. الاختيار عبر الماوس
        popupListView.setOnMouseClicked(event -> {
            LocalCustomer selected = popupListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectCustomer(selected);
            }
        });

        // 2. الاختيار عبر زر Enter من داخل القائمة المنسدلة
        popupListView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    LocalCustomer selected = popupListView.getSelectionModel().getSelectedItem();
                    if (selected != null) selectCustomer(selected);
                    break;
                case ESCAPE:
                    autocompletePopup.hide();
                    txtCustomerSearch.requestFocus();
                    break;
                default:
                    break;
            }
        });

        autocompletePopup.getContent().add(popupListView);

        // 3. التحكم بالقائمة من خلال حقل البحث (الأسهم + Enter)
        txtCustomerSearch.setOnKeyPressed(event -> {
            if (autocompletePopup.isShowing()) {
                switch (event.getCode()) {
                    case DOWN:
                        // الانتقال للقائمة عند ضغط السهم السفلي
                        popupListView.requestFocus();
                        popupListView.getSelectionModel().selectFirst();
                        break;
                    case ENTER:
                        // اختيار أول زبون في القائمة إذا ضغط Enter وهو لا يزال في حقل النص
                        if (!popupListView.getItems().isEmpty()) {
                            selectCustomer(popupListView.getItems().get(0));
                        }
                        break;
                    case ESCAPE:
                        autocompletePopup.hide();
                        break;
                    default:
                        break;
                }
            }
        });

        txtCustomerSearch.textProperty().addListener((obs, oldText, newText) -> {
            if (isSelecting) return;
            
            if (newText == null || newText.trim().isEmpty()) {
                autocompletePopup.hide();
                currentVerifiedCustomerId = null;
                lblTotalDebt.setText("0.00 ل.س");
                currentTotalDebt = BigDecimal.ZERO;
                unpaidSalesForCustomer.clear();
                return;
            }

            String search = newText.toLowerCase().trim();
            List<LocalCustomer> filtered = new ArrayList<>();
            for (LocalCustomer c : allCustomers) {
                if (c.getName().toLowerCase().contains(search) || 
                   (c.getPhone() != null && c.getPhone().contains(search))) {
                    filtered.add(c);
                }
            }

            if (!filtered.isEmpty()) {
                popupListView.setItems(FXCollections.observableArrayList(filtered));
                popupListView.prefWidthProperty().bind(txtCustomerSearch.widthProperty());
                
                if (!autocompletePopup.isShowing()) {
                    javafx.geometry.Bounds bounds = txtCustomerSearch.localToScreen(txtCustomerSearch.getBoundsInLocal());
                    autocompletePopup.show(txtCustomerSearch, bounds.getMinX(), bounds.getMaxY());
                }
            } else {
                autocompletePopup.hide();
                currentVerifiedCustomerId = null;
                lblTotalDebt.setText("0.00 ل.س");
            }
        });
    }

    private void selectCustomer(LocalCustomer customer) {
        isSelecting = true;
        txtCustomerSearch.setText(customer.getName());
        isSelecting = false;
        
        autocompletePopup.hide();

        currentVerifiedCustomerId = customer.getCustomer_id();
        
        // جلب ديون العميل فور اختياره
        calculateCustomerDebt(customer.getCustomer_id());
    }

    private void calculateCustomerDebt(int customerId) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    LocalDate safeStart = LocalDate.of(2000, 1, 1);
                    LocalDate safeEnd = LocalDate.now().plusDays(1);
                    
                    List<Sale> allSales = saleService.getSalesByDateRange(safeStart, safeEnd); 
                    
                    unpaidSalesForCustomer.clear();
                    currentTotalDebt = BigDecimal.ZERO;

                    for (Sale sale : allSales) {
                        if (sale.getCustomer_id() != null && sale.getCustomer_id() == customerId) {
                            BigDecimal debt = sale.getTotal_customer_debt() != null ? sale.getTotal_customer_debt() : BigDecimal.ZERO;
                            if (debt.compareTo(BigDecimal.ZERO) > 0) {
                                unpaidSalesForCustomer.add(sale);
                                currentTotalDebt = currentTotalDebt.add(debt);
                            }
                        }
                    }

                    unpaidSalesForCustomer.sort((s1, s2) -> s1.getSale_date().compareTo(s2.getSale_date()));

                    Platform.runLater(() -> {
                        lblTotalDebt.setText(String.format("%,.2f ل.س", currentTotalDebt));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> AlertManager.showError("خطأ", "فشل حساب ديون العميل:\n" + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleSavePayment(ActionEvent event) {
        if (currentVerifiedCustomerId == null) {
            AlertManager.showWarning("تنبيه", "يرجى البحث عن الزبون واختياره من القائمة المنسدلة أولاً.");
            return;
        }

        if (SessionManager.getInstance().getCurrentShift() == null) {
            AlertManager.showError("خطأ", "لا توجد وردية مفتوحة حالياً. يرجى فتح وردية أولاً.");
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

            CustomerPayment payment = new CustomerPayment();
            payment.setCustomer_id(currentVerifiedCustomerId);
            payment.setSale_id(null); 
            payment.setShift_id(currentShiftId);
            payment.setAmount_paid(paymentAmount);
            
            paymentService.createPayment(payment);

            BigDecimal remainingPayment = paymentAmount;

            for (Sale sale : unpaidSalesForCustomer) {
                if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal currentSaleDebt = sale.getTotal_customer_debt();

                if (remainingPayment.compareTo(currentSaleDebt) >= 0) {
                    remainingPayment = remainingPayment.subtract(currentSaleDebt);
                    sale.setTotal_customer_debt(BigDecimal.ZERO);
                    
                    BigDecimal insDebt = sale.getTotal_insurance_debt() != null ? sale.getTotal_insurance_debt() : BigDecimal.ZERO;
                    if (insDebt.compareTo(BigDecimal.ZERO) == 0) {
                        sale.setStatus("Completed");
                    }
                } else {
                    sale.setTotal_customer_debt(currentSaleDebt.subtract(remainingPayment));
                    remainingPayment = BigDecimal.ZERO;
                }

                saleService.updateSale(sale);
            }

            AlertManager.showSuccess("تم بنجاح", "تم استلام الدفعة وتسوية الديون تلقائياً.");
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