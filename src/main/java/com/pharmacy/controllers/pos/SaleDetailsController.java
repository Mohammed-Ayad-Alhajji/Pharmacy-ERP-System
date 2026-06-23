package com.pharmacy.controllers.pos;

import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.pos.InsuranceCompany;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.models.pos.PatientReturn;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.models.pos.SaleDetail;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.PatientReturnService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.utils.gui.AlertManager;
import com.pharmacy.utils.export.ExportUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleDetailsController {

    @FXML private Label lblSaleId, lblDate, lblCashier, lblCustomer;
    @FXML private Label lblPrescription, lblInsurance;
    @FXML private Label lblTotalAmount, lblTotalInsurance, lblDiscount, lblRounding, lblNetTotal, lblPaidAmount;
    @FXML private Label lblCustomerDebt, lblInsuranceDebt, lblPaymentStatus;
    @FXML private VBox vboxDebts;

    @FXML private TableView<SaleDetailWrapper> tvDetails;
    @FXML private TableColumn<SaleDetailWrapper, Integer> colIndex;
    @FXML private TableColumn<SaleDetailWrapper, String> colItemName, colItemBatch, colItemUnit;
    @FXML private TableColumn<SaleDetailWrapper, Integer> colItemQty, colItemReturned; // <--- تمت إضافة عمود المرتجع
    @FXML private TableColumn<SaleDetailWrapper, String> colItemPrice, colItemSubtotal, colItemInsuranceShare, colItemPatientShare;

    private Sale currentSale;
    private String cashierName; 
    
    private SaleService saleService;
    private LocalCustomerService customerService;
    private BatchService batchService;
    private MedicineService medicineService;
    private InsuranceCompanyService insuranceService;
    private PatientReturnService returnService; // <--- الخدمة الجديدة

    // تم تحديث توقيع الدالة لتستقبل PatientReturnService
    public void initData(Sale sale, String cashierName, SaleService sService, UserService uService, LocalCustomerService cService, BatchService bService, MedicineService mService, InsuranceCompanyService iService, PatientReturnService rService) {
        this.currentSale = sale;
        this.cashierName = cashierName;
        this.saleService = sService;
        this.customerService = cService;
        this.batchService = bService;
        this.medicineService = mService;
        this.insuranceService = iService;
        this.returnService = rService;

        setupTableColumns();
        loadAllDataInBackground();
    }

    private void setupTableColumns() {
        colIndex.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tvDetails.getItems().indexOf(column.getValue()) + 1));
        colItemName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medicineName));
        colItemBatch.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().batchNumber));
        colItemUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().unitType));
        
        colItemQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().detail.getQuantity_sold()));
        
        // ربط عمود المرتجع
        colItemReturned.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().returnedQty));
        
        colItemPrice.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().detail.getUnit_sell_price())));
        
        colItemSubtotal.setCellValueFactory(c -> {
            BigDecimal qty = BigDecimal.valueOf(c.getValue().detail.getQuantity_sold());
            BigDecimal unitPrice = c.getValue().detail.getUnit_sell_price();
            return new SimpleStringProperty(formatMoney(qty.multiply(unitPrice)));
        });

        colItemInsuranceShare.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().detail.getInsurance_share())));
        colItemPatientShare.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().detail.getPatient_share())));
    }

    private void loadAllDataInBackground() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    String customerName = "زبون عام (نقدي)";
                    Integer custId = currentSale.getCustomer_id();
                    if (custId != null && custId > 0 && customerService != null) {
                        Optional<LocalCustomer> optCust = customerService.getCustomerById(custId);
                        if (optCust.isPresent()) customerName = optCust.get().getName();
                    }

                    String insuranceName = "لا يوجد تغطية تأمينية";
                    boolean hasInsurance = false;
                    Integer insId = currentSale.getInsurance_id();
                    if (insId != null && insId > 0 && insuranceService != null) {
                        Optional<InsuranceCompany> optIns = insuranceService.getCompanyById(insId);
                        if (optIns.isPresent()) {
                            insuranceName = optIns.get().getName() + " (الموافقة: " + currentSale.getInsurance_approval_code() + ")";
                            hasInsurance = true;
                        }
                    }

                    List<SaleDetail> details = saleService.getSaleDetailsBySaleId(currentSale.getSale_id());
                    List<SaleDetailWrapper> wrappers = new ArrayList<>();
                    
                    BigDecimal originalTotalInsuranceShare = BigDecimal.ZERO;

                    for (SaleDetail detail : details) {
                        String mName = "دواء محذوف";
                        String bNumber = "N/A";
                        String uType = "غير محدد"; 

                        if (batchService != null) {
                            Optional<Batch> optBatch = batchService.getBatchById(detail.getBatch_id());
                            if (optBatch.isPresent()) {
                                bNumber = optBatch.get().getBatch_number();
                                if (medicineService != null) {
                                    Optional<Medicine> optMed = medicineService.getMedicineById(optBatch.get().getMed_id());
                                    if (optMed.isPresent()) {
                                        Medicine m = optMed.get();
                                        mName = m.getBrand_name();
                                        
                                        BigDecimal soldPrice = detail.getUnit_sell_price();
                                        if (soldPrice.compareTo(m.getCurrent_box_sell_price()) == 0 || m.getConversion_factor() <= 1) {
                                            uType = "علبة";
                                        } else if (soldPrice.compareTo(m.getCurrent_unit_sell_price()) == 0) {
                                            uType = "ظرف/قطعة";
                                        } else {
                                            uType = "مخصصة"; 
                                        }
                                    }
                                }
                            }
                        }
                        
                        // --- البحث عن أي مرتجعات تمت على هذا السطر ---
                        int totalReturnedForThisItem = 0;
                        if(returnService != null) {
                            List<PatientReturn> returns = returnService.getReturnsBySaleDetail(detail.getDetail_id());
                            for(PatientReturn pr : returns) {
                                totalReturnedForThisItem += pr.getQuantity_returned();
                            }
                        }

                        BigDecimal insShare = detail.getInsurance_share() != null ? detail.getInsurance_share() : BigDecimal.ZERO;
                        originalTotalInsuranceShare = originalTotalInsuranceShare.add(insShare);

                        wrappers.add(new SaleDetailWrapper(detail, mName, bNumber, uType, totalReturnedForThisItem)); // تمرير المرتجع للـ Wrapper
                    }

                    final String finalCustName = customerName;
                    final String finalInsName = insuranceName;
                    final boolean finalHasIns = hasInsurance;
                    final BigDecimal finalInsShareTotal = originalTotalInsuranceShare;

                    Platform.runLater(() -> populateUI(finalCustName, finalInsName, finalHasIns, finalInsShareTotal, wrappers));

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> AlertManager.showError("خطأ", "تعذر جلب تفاصيل الفاتورة بشكل كامل."));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void populateUI(String custName, String insName, boolean hasIns, BigDecimal originalTotalInsuranceShare, List<SaleDetailWrapper> wrappers) {
        lblSaleId.setText(String.valueOf(currentSale.getSale_id()));
        lblCashier.setText(this.cashierName != null ? this.cashierName : "غير معروف");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        lblDate.setText(currentSale.getSale_date() != null ? currentSale.getSale_date().format(formatter) : "");
        lblCustomer.setText(custName);

        String doctor = currentSale.getDoctor_name();
        String patient = currentSale.getPatient_name();
        if ((doctor != null && !doctor.isEmpty()) || (patient != null && !patient.isEmpty())) {
            String docText = doctor != null && !doctor.isEmpty() ? doctor : "---";
            String patText = patient != null && !patient.isEmpty() ? patient : "---";
            lblPrescription.setText("د. " + docText + " / المريض: " + patText);
        } else {
            lblPrescription.setText("بدون وصفة طبيب");
        }

        lblInsurance.setText(insName);

        BigDecimal total = safeBigDecimal(currentSale.getTotal_amount());
        BigDecimal discount = safeBigDecimal(currentSale.getDiscount_amount());
        BigDecimal rounding = safeBigDecimal(currentSale.getRounding_adjustment());
        BigDecimal currentCustDebt = safeBigDecimal(currentSale.getTotal_customer_debt());
        BigDecimal currentInsDebt = safeBigDecimal(currentSale.getTotal_insurance_debt());
        
        BigDecimal netRequired = total.subtract(originalTotalInsuranceShare).subtract(discount).add(rounding);
        if (netRequired.compareTo(BigDecimal.ZERO) < 0) netRequired = BigDecimal.ZERO;
        
        BigDecimal totalPaidSoFar = netRequired.subtract(currentCustDebt);
        if (totalPaidSoFar.compareTo(BigDecimal.ZERO) < 0) totalPaidSoFar = BigDecimal.ZERO;
        
        lblTotalAmount.setText(formatMoney(total));
        lblTotalInsurance.setText(formatMoney(originalTotalInsuranceShare));
        lblDiscount.setText(formatMoney(discount));
        
        if (rounding.compareTo(BigDecimal.ZERO) > 0) {
            lblRounding.setText("+" + formatMoney(rounding));
        } else {
            lblRounding.setText(formatMoney(rounding));
        }
        
        lblNetTotal.setText(formatMoney(netRequired));
        lblPaidAmount.setText(formatMoney(totalPaidSoFar));

        if (currentCustDebt.compareTo(BigDecimal.ZERO) == 0 && currentInsDebt.compareTo(BigDecimal.ZERO) == 0) {
            vboxDebts.setVisible(false);
            vboxDebts.setManaged(false);
            
            lblPaymentStatus.setText("مسددة بالكامل");
            lblPaymentStatus.setStyle("-fx-background-color: #e8f8f5; -fx-text-fill: #27ae60; -fx-border-color: #27ae60; -fx-font-weight: bold;"); 
        } else {
            vboxDebts.setVisible(true);
            vboxDebts.setManaged(true);
            
            lblCustomerDebt.setText(formatMoney(currentCustDebt));
            lblInsuranceDebt.setText(formatMoney(currentInsDebt));
            
            lblPaymentStatus.setText("يوجد ذمم معلقة");
            lblPaymentStatus.setStyle("-fx-background-color: #fdf2e9; -fx-text-fill: #e67e22; -fx-border-color: #e67e22; -fx-font-weight: bold;"); 
        }

        tvDetails.setItems(FXCollections.observableArrayList(wrappers));
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String formatMoney(BigDecimal amount) {
        return amount != null ? String.format("%,.2f ل.س", amount) : "0.00 ل.س";
    }

    @FXML
    private void handlePrint(ActionEvent event) {
        AlertManager.showSuccess("جاري الطباعة", "جاري تحضير الفاتورة رقم " + currentSale.getSale_id() + " للطباعة الحرارية...");
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) lblSaleId.getScene().getWindow();
        stage.close();
    }

    public static class SaleDetailWrapper {
        SaleDetail detail;
        String medicineName;
        String batchNumber;
        String unitType; 
        int returnedQty; // الحقل الجديد للمرتجعات

        public SaleDetailWrapper(SaleDetail detail, String medicineName, String batchNumber, String unitType, int returnedQty) {
            this.detail = detail;
            this.medicineName = medicineName;
            this.batchNumber = batchNumber;
            this.unitType = unitType;
            this.returnedQty = returnedQty;
        }
    }
    // ==========================================
    // دوال التصدير (Export)
    // ==========================================
    
    @FXML
    private void handleExportExcel(ActionEvent event) {
        if (currentSale != null && !tvDetails.getItems().isEmpty()) {
            String fileName = "تفاصيل_مبيعات_رقم_" + currentSale.getSale_id();
            ExportUtils.exportToExcel(tvDetails, fileName);
        } else {
            AlertManager.showError("تنبيه", "لا توجد بيانات لتصديرها.");
        }
    }

    @FXML
    private void handleExportPDF(ActionEvent event) {
        if (currentSale != null && !tvDetails.getItems().isEmpty()) {
            String title = "البيان التفصيلي للفاتورة رقم: " + currentSale.getSale_id();
            ExportUtils.exportToPDF(tvDetails, title);
        } else {
            AlertManager.showError("تنبيه", "لا توجد بيانات لتصديرها.");
        }
    }
}