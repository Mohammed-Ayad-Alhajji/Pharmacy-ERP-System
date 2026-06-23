package com.pharmacy.controllers.finance;

import com.pharmacy.dao.impl.finance.CustomerPaymentDAOImpl;
import com.pharmacy.dao.impl.pos.LocalCustomerDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDetailDAOImpl;
import com.pharmacy.models.finance.CustomerPayment;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.services.impl.finance.CustomerPaymentServiceImpl;
import com.pharmacy.services.impl.pos.LocalCustomerServiceImpl;
import com.pharmacy.services.impl.pos.SaleServiceImpl;
import com.pharmacy.services.interfaces.finance.CustomerPaymentService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CustomerPaymentsController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private DatePicker dpStart, dpEnd;
    @FXML private TableView<PaymentWrapper> tvPayments;
    @FXML private TableColumn<PaymentWrapper, Integer> colIndex;
    @FXML private TableColumn<PaymentWrapper, String> colPaymentId, colType, colTarget, colAmount, colDate, colShift;
    
    @FXML private ComboBox<Integer> cbRowsPerPage;
    @FXML private Button btnPrev, btnNext;
    @FXML private Label lblPageInfo;

    private List<PaymentWrapper> masterList = new ArrayList<>();
    private List<PaymentWrapper> currentFilteredList = new ArrayList<>();
    private int currentPage = 1;
    private int rowsPerPage = 50;

    private CustomerPaymentService paymentService;
    private LocalCustomerService customerService;
    private SaleService saleService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initServices();
        setupTable();
        setupPaginationControls();
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> { currentPage = 1; updatePagination(); });
        loadInitialData();
    }

    private void initServices() {
        this.paymentService = new CustomerPaymentServiceImpl(new CustomerPaymentDAOImpl());
        this.customerService = new LocalCustomerServiceImpl(new LocalCustomerDAOImpl());
        this.saleService = new SaleServiceImpl(new SaleDAOImpl(), new SaleDetailDAOImpl());
    }

    private void setupPaginationControls() {
        cbRowsPerPage.getItems().addAll(25, 50, 100, 500);
        cbRowsPerPage.setValue(50);
        cbRowsPerPage.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) { rowsPerPage = newVal; currentPage = 1; updatePagination(); }
        });
    }

    private void setupTable() {
        colIndex.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(tvPayments.getItems().indexOf(c.getValue()) + 1 + ((currentPage - 1) * rowsPerPage)));
        colPaymentId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().paymentId));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type));
        colTarget.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().target));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().amount)));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date));
        colShift.setCellValueFactory(c -> new SimpleStringProperty("وردية #" + c.getValue().shiftId));
    }

    @FXML
    public void loadInitialData() {
        dpStart.setValue(LocalDate.now().minusDays(30)); 
        dpEnd.setValue(LocalDate.now());
        txtSearch.clear();
        handleSearch();
    }

    @FXML
    private void handleSearch() {
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();
        if (start == null || end == null) return;
        LocalDate adjustedEnd = end.plusDays(1);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<CustomerPayment> payments = paymentService.getPaymentsByDateRange(start, adjustedEnd);
                    
                    // فرز من الأحدث للأقدم
                    payments.sort((p1, p2) -> p2.getPayment_date().compareTo(p1.getPayment_date()));

                    List<PaymentWrapper> wrappers = new ArrayList<>();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");

                    for (CustomerPayment p : payments) {
                        String type, target;
                        // تحليل قاعدة الـ XOR للعرض
                        if (p.getCustomer_id() != null && p.getCustomer_id() > 0) {
                            type = "دفعة عامة (حساب عميل)";
                            Optional<LocalCustomer> cust = customerService.getCustomerById(p.getCustomer_id());
                            target = cust.isPresent() ? cust.get().getName() : "زبون مجهول (" + p.getCustomer_id() + ")";
                        } else {
                            type = "تسديد فاتورة محددة";
                            target = "فاتورة رقم: " + p.getSale_id();
                        }

                        String pDate = p.getPayment_date() != null ? p.getPayment_date().format(formatter) : "";
                        wrappers.add(new PaymentWrapper("TRX-" + p.getPayment_id(), type, target, p.getAmount_paid(), pDate, p.getShift_id()));
                    }

                    Platform.runLater(() -> {
                        masterList.clear();
                        masterList.addAll(wrappers);
                        currentPage = 1;
                        updatePagination();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> AlertManager.showError("خطأ", "فشل جلب سجل المقبوضات:\n" + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void updatePagination() {
        String search = txtSearch.getText().toLowerCase().trim();
        currentFilteredList.clear();

        for (PaymentWrapper pw : masterList) {
            if (search.isEmpty() || pw.paymentId.toLowerCase().contains(search) || pw.target.toLowerCase().contains(search)) {
                currentFilteredList.add(pw);
            }
        }

        int totalItems = currentFilteredList.size();
        int totalPages = (int) Math.ceil((double) totalItems / rowsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        tvPayments.getItems().setAll(currentFilteredList.subList(fromIndex, toIndex));
        lblPageInfo.setText("صفحة " + currentPage + " من " + totalPages);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
        
        colIndex.setVisible(false); colIndex.setVisible(true);
    }

    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; updatePagination(); } }
    @FXML private void handleNextPage() { 
        int totalPages = (int) Math.ceil((double) currentFilteredList.size() / rowsPerPage);
        if (currentPage < totalPages) { currentPage++; updatePagination(); }
    }

    @FXML
    private void handleCreateNewPayment(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/views/finance/CreateCustomerPaymentView.fxml"));
            Parent root = loader.load();
            
            CreateCustomerPaymentController controller = loader.getController();
            controller.initServices(paymentService, customerService, saleService);

            Stage stage = new Stage();
            stage.setTitle("تسجيل دفعة من زبون");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); 
            
            stage.showAndWait(); 
            handleSearch(); 

        } catch (Exception e) {
            e.printStackTrace();
            AlertManager.showError("خطأ", "تعذر فتح نافذة تسجيل المقبوضات.");
        }
    }

    private String formatMoney(BigDecimal amount) {
        return amount != null ? String.format("%,.2f ل.س", amount) : "0.00 ل.س";
    }

    public static class PaymentWrapper {
        String paymentId, type, target, date;
        BigDecimal amount;
        int shiftId;

        public PaymentWrapper(String paymentId, String type, String target, BigDecimal amount, String date, int shiftId) {
            this.paymentId = paymentId; this.type = type; this.target = target;
            this.amount = amount; this.date = date; this.shiftId = shiftId;
        }
    }
}