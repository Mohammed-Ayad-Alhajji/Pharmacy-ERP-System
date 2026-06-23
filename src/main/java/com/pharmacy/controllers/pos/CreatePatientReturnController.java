package com.pharmacy.controllers.pos;

import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.pos.InsuranceCompany;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.models.pos.PatientReturn;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.models.pos.SaleDetail;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.PatientReturnService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CreatePatientReturnController implements Initializable {

    @FXML private TextField txtSaleId;
    @FXML private Label lblCustomer, lblDate, lblTotalInvoice, lblTotalRefund;
    @FXML private TextField txtReason;

    @FXML private TableView<ReturnItemWrapper> tvReturnItems;
    @FXML private TableColumn<ReturnItemWrapper, Integer> colIndex, colOriginalQty, colPastReturnQty, colAvailableQty, colReturnQty;
    @FXML private TableColumn<ReturnItemWrapper, String> colMedicine, colBatch, colUnit, colPatientUnitPrice, colRefundAmount;
    @FXML private TableColumn<ReturnItemWrapper, String> colInsuranceCompany, colInsuranceShare; 

    private ObservableList<ReturnItemWrapper> itemsList = FXCollections.observableArrayList();
    private Sale currentSale;
    
    private SaleService saleService;
    private MedicineService medicineService;
    private BatchService batchService;
    private LocalCustomerService customerService;
    private PatientReturnService returnService;
    private InsuranceCompanyService insuranceService;

    public void initServices(SaleService sService, MedicineService mService, BatchService bService, 
                             LocalCustomerService cService, PatientReturnService rService, InsuranceCompanyService iService) {
        this.saleService = sService;
        this.medicineService = mService;
        this.batchService = bService;
        this.customerService = cService;
        this.returnService = rService;
        this.insuranceService = iService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
    }

    private void setupTable() {
        tvReturnItems.setItems(itemsList);
        colIndex.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tvReturnItems.getItems().indexOf(column.getValue()) + 1));
        colMedicine.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medicineName));
        colBatch.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().batchNumber));
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().unitType));
        
        colOriginalQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().originalQty));
        colPastReturnQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().pastReturnQty));
        colAvailableQty.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().availableQty));
        
        // إعادة تفعيل أعمدة التأمين لكي يميز الكاشير بين الدواء المؤمن والغير مؤمن
        if(colInsuranceCompany != null) colInsuranceCompany.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().insuranceCompanyName));
        if(colInsuranceShare != null) colInsuranceShare.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().unitInsuranceShare)));
        
        colPatientUnitPrice.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().unitSellPrice))); 
        colRefundAmount.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().rowRefundAmount.get())));
        
        colReturnQty.setCellValueFactory(c -> c.getValue().qtyToReturn.asObject());
        colReturnQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        
        colReturnQty.setOnEditCommit(event -> {
            ReturnItemWrapper item = event.getRowValue();

            // --- جدار الحماية على مستوى السطر (Item-Level Block) ---
            if (item.isCoveredByInsurance) {
                AlertManager.showError("مرفوض", "هذا الدواء تحديداً تم تغطيته من قبل شركة التأمين، يمنع إرجاعه من هنا. يمكنك إرجاع الأدوية الأخرى في الفاتورة إن وجدت.");
                item.setQtyToReturn(0);
                tvReturnItems.refresh();
                return;
            }

            int newValue = (event.getNewValue() != null) ? event.getNewValue() : 0;
            if (newValue < 0) newValue = 0;
            if (newValue > item.availableQty) {
                AlertManager.showWarning("تنبيه", "الكمية المطلوبة أكبر من المتاح للإرجاع!");
                newValue = item.availableQty;
            }
            
            item.setQtyToReturn(newValue);
            tvReturnItems.refresh(); 
            calculateTotalRefund(); 
        });
    }

    @FXML
    private void handleFetchInvoice(ActionEvent event) {
        String saleIdStr = txtSaleId.getText().trim();
        if (saleIdStr.isEmpty()) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Optional<Sale> optSale = saleService.getSaleById(Integer.parseInt(saleIdStr));
                    Platform.runLater(() -> {
                        if (optSale.isEmpty()) {
                            AlertManager.showError("خطأ", "الفاتورة غير موجودة.");
                            return;
                        }
                        
                        currentSale = optSale.get();
                        
                        // 1. جلب اسم الزبون
                        Integer custId = currentSale.getCustomer_id();
                        if (custId != null && custId > 0 && customerService != null) {
                            Optional<LocalCustomer> optCust = customerService.getCustomerById(custId);
                            lblCustomer.setText(optCust.isPresent() ? optCust.get().getName() : "زبون عام");
                        } else {
                            lblCustomer.setText("زبون عام");
                        }

                        // 2. جلب اسم شركة التأمين للفاتورة (إن وجدت)
                        String invoiceInsName = "لا يوجد";
                        if (currentSale.getInsurance_id() != null && currentSale.getInsurance_id() > 0 && insuranceService != null) {
                            Optional<InsuranceCompany> optIns = insuranceService.getCompanyById(currentSale.getInsurance_id());
                            if (optIns.isPresent()) invoiceInsName = optIns.get().getName();
                        }

                        lblDate.setText(currentSale.getSale_date() != null ? currentSale.getSale_date().format(DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm")) : "");
                        lblTotalInvoice.setText(formatMoney(currentSale.getTotal_amount()));

                        itemsList.clear();
                        boolean anyItemAvailable = false;

                        List<SaleDetail> details = saleService.getSaleDetailsBySaleId(currentSale.getSale_id());
                        for (SaleDetail detail : details) {
                            String medName = "دواء غير معروف";
                            String bNum = "N/A";
                            String uType = "غير محدد";

                            if (batchService != null) {
                                Optional<Batch> optB = batchService.getBatchById(detail.getBatch_id());
                                if (optB.isPresent()) {
                                    bNum = optB.get().getBatch_number();
                                    if (medicineService != null) {
                                        Optional<Medicine> optM = medicineService.getMedicineById(optB.get().getMed_id());
                                        if (optM.isPresent()) {
                                            Medicine m = optM.get();
                                            medName = m.getBrand_name();
                                            
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

                            int pastQty = 0;
                            if (returnService != null) {
                                List<PatientReturn> pasts = returnService.getReturnsBySaleDetail(detail.getDetail_id());
                                for (PatientReturn pr : pasts) {
                                    pastQty += pr.getQuantity_returned();
                                }
                            }

                            int available = detail.getQuantity_sold() - pastQty;
                            if (available > 0) anyItemAvailable = true;

                            // 3. التحقق من التأمين على مستوى السطر
                            BigDecimal unitInsShare = BigDecimal.ZERO;
                            if (detail.getQuantity_sold() > 0 && detail.getInsurance_share() != null) {
                                unitInsShare = detail.getInsurance_share().divide(BigDecimal.valueOf(detail.getQuantity_sold()), 2, RoundingMode.HALF_UP);
                            }
                            
                            boolean isCovered = unitInsShare.compareTo(BigDecimal.ZERO) > 0;
                            String rowInsName = isCovered ? invoiceInsName : "لا يوجد";

                            itemsList.add(new ReturnItemWrapper(
                                detail.getDetail_id(), medName, bNum, uType,
                                detail.getQuantity_sold(), pastQty, available, 0, 
                                detail.getUnit_sell_price(), unitInsShare, rowInsName, isCovered
                            ));
                        }

                        if (!anyItemAvailable) {
                            AlertManager.showWarning("تنبيه", "لقد تم إرجاع جميع أدوية هذه الفاتورة مسبقاً.");
                        }
                        calculateTotalRefund();
                    });
                } catch (Exception e) { e.printStackTrace(); }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void calculateTotalRefund() {
        if (currentSale == null) return;
        
        BigDecimal cashRefundAmount = BigDecimal.ZERO;

        for (ReturnItemWrapper item : itemsList) {
            BigDecimal qtyToReturn = BigDecimal.valueOf(item.qtyToReturn.get());
            BigDecimal rowRefund = item.unitSellPrice.multiply(qtyToReturn);
            
            item.rowRefundAmount.set(rowRefund); 
            cashRefundAmount = cashRefundAmount.add(rowRefund);
        }

        double refundDouble = cashRefundAmount.doubleValue();
        double roundedDouble = Math.ceil(refundDouble / 5.0) * 5.0;
        BigDecimal finalRefund = BigDecimal.valueOf(roundedDouble);

        lblTotalRefund.setText(formatMoney(finalRefund));
    }

    @FXML
    private void handleSaveReturn(ActionEvent event) {
        String refundText = lblTotalRefund.getText().replace(" ل.س", "").replace(",", "").trim();
        BigDecimal finalRefund = new BigDecimal(refundText);

        if (finalRefund.compareTo(BigDecimal.ZERO) <= 0) {
            AlertManager.showWarning("تنبيه", "لا توجد كميات محددة للإرجاع.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("تأكيد عملية المرتجع");
        alert.setHeaderText("تأكيد دفع الاسترداد النقدي");
        alert.setContentText("سيتم إخراج مبلغ قدره: " + formatMoney(finalRefund) + " من الدرج وتسليمه للزبون نقداً.\nهل أنت متأكد من المتابعة؟");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean assignedCash = false;
                for (ReturnItemWrapper item : itemsList) {
                    if (item.qtyToReturn.get() > 0) {
                        PatientReturn pr = new PatientReturn();
                        pr.setDetail_id(item.detailId);
                        pr.setShift_id(SessionManager.getInstance().getCurrentShift().getShift_id());
                        pr.setQuantity_returned(item.qtyToReturn.get());
                        
                        pr.setPatient_cash_refund(!assignedCash ? finalRefund : BigDecimal.ZERO);
                        pr.setReason(txtReason.getText().trim());
                        
                        returnService.createReturn(pr);
                        assignedCash = true;
                    }
                }
                AlertManager.showSuccess("تم بنجاح", "تم حفظ المرتجع وإثبات النقص في صندوق الوردية بنجاح.");
                ((Stage)txtSaleId.getScene().getWindow()).close();
            } catch (Exception e) { 
                e.printStackTrace(); 
                AlertManager.showError("خطأ", e.getMessage()); 
            }
        }
    }

    private String formatMoney(BigDecimal amount) {
        return amount != null ? String.format("%,.2f ل.س", amount) : "0.00 ل.س";
    }

    public static class ReturnItemWrapper {
        int detailId, originalQty, pastReturnQty, availableQty;
        String medicineName, batchNumber, unitType, insuranceCompanyName;
        BigDecimal unitSellPrice, unitInsuranceShare;
        boolean isCoveredByInsurance;
        SimpleIntegerProperty qtyToReturn;
        SimpleObjectProperty<BigDecimal> rowRefundAmount;

        public ReturnItemWrapper(int detailId, String med, String bch, String unitType, int orig, int past, int avail, int ret, 
                                 BigDecimal price, BigDecimal insShare, String insName, boolean isCovered) {
            this.detailId = detailId; 
            this.medicineName = med; 
            this.batchNumber = bch;
            this.unitType = unitType; 
            this.originalQty = orig; 
            this.pastReturnQty = past; 
            this.availableQty = avail;
            this.unitSellPrice = price; 
            this.unitInsuranceShare = insShare;
            this.insuranceCompanyName = insName;
            this.isCoveredByInsurance = isCovered;
            this.qtyToReturn = new SimpleIntegerProperty(ret);
            this.rowRefundAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
        }
        public void setQtyToReturn(int q) { this.qtyToReturn.set(q); }
    }
}