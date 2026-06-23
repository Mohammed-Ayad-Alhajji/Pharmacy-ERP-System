package com.pharmacy.controllers.pos;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.pos.InsuranceCompanyDAOImpl;
import com.pharmacy.dao.impl.pos.LocalCustomerDAOImpl;
import com.pharmacy.dao.impl.pos.PatientReturnDAOImpl; // تم الإضافة
import com.pharmacy.dao.impl.pos.SaleDAOImpl;
import com.pharmacy.dao.impl.pos.SaleDetailDAOImpl;
import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.models.pos.SaleDetail;
import com.pharmacy.models.security.User;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.impl.pos.InsuranceCompanyServiceImpl;
import com.pharmacy.services.impl.pos.LocalCustomerServiceImpl;
import com.pharmacy.services.impl.pos.PatientReturnServiceImpl; // تم الإضافة
import com.pharmacy.services.impl.pos.SaleServiceImpl;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.interfaces.inventory.BatchService;
import com.pharmacy.services.interfaces.inventory.MedicineService;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;
import com.pharmacy.services.interfaces.pos.PatientReturnService; // تم الإضافة
import com.pharmacy.services.interfaces.pos.SaleService;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SalesManagementController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private HBox hboxManagerFilters; 
    @FXML private DatePicker dpStart, dpEnd;
    @FXML private MenuButton mbCashier; 
    @FXML private ComboBox<String> cbPaymentMethod;

    @FXML private TableView<SaleWrapper> tvSales;
    @FXML private TableColumn<SaleWrapper, Integer> colSaleId;
    @FXML private TableColumn<SaleWrapper, String> colDate, colCashier, colCustomer, colPaymentMethod, colStatus;
    @FXML private TableColumn<SaleWrapper, BigDecimal> colTotal;
    @FXML private TableColumn<SaleWrapper, Void> colActions;

    private SaleService saleService;
    private UserService userService;
    private LocalCustomerService customerService;
    private BatchService batchService;
    private MedicineService medicineService;
    private InsuranceCompanyService insuranceService;
    private PatientReturnService returnService; // <--- تم تعريف الخدمة هنا

    private User currentUser;
    private ObservableList<SaleWrapper> masterSalesList = FXCollections.observableArrayList();
    private FilteredList<SaleWrapper> filteredSalesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        initServices();
        
        setupTables();
        setupPermissions();

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyLocalFilters());
        cbPaymentMethod.valueProperty().addListener((obs, oldVal, newVal) -> applyLocalFilters());

        loadInitialData();
    }

    private void initServices() {
        this.saleService = new SaleServiceImpl(new SaleDAOImpl(), new SaleDetailDAOImpl());
        this.userService = new UserServiceImpl(new UserDAOImpl());
        this.customerService = new LocalCustomerServiceImpl(new LocalCustomerDAOImpl());
        this.batchService = new BatchServiceImpl(new BatchDAOImpl());
        this.medicineService = new MedicineServiceImpl(new MedicineDAOImpl());
        this.insuranceService = new InsuranceCompanyServiceImpl(new InsuranceCompanyDAOImpl());
        
        // <--- تم تهيئة الخدمة هنا لكي لا يحدث خطأ NullPointerException
        this.returnService = new PatientReturnServiceImpl(new PatientReturnDAOImpl()); 
    }

    private void setupPermissions() {
        cbPaymentMethod.getItems().addAll("الكل", "نقدي", "ذمة مالية");
        cbPaymentMethod.getSelectionModel().selectFirst();

        boolean isManager = SessionManager.getInstance().hasPermission("pos_view_all_sales");

        if (!isManager) {
            hboxManagerFilters.setVisible(false);
            hboxManagerFilters.setManaged(false);

            dpStart.setValue(LocalDate.now().minusMonths(1)); 
            dpEnd.setValue(LocalDate.now());
        } else {
            hboxManagerFilters.setVisible(true);
            hboxManagerFilters.setManaged(true);
            
            mbCashier.getItems().clear();
            try {
                List<User> allUsers = userService.getUsers(false, 1000, 0); 
                for (User u : allUsers) {
                    CheckMenuItem checkItem = new CheckMenuItem(u.getFull_name());
                    checkItem.setUserData(u); 
                    checkItem.selectedProperty().addListener((obs, wasSelected, isSelected) -> applyLocalFilters());
                    mbCashier.getItems().add(checkItem);
                }
            } catch (Exception e) {
                System.out.println("تنبيه: تعذر جلب قائمة المستخدمين.");
            }

            dpStart.setValue(LocalDate.now().minusMonths(1)); 
            dpEnd.setValue(LocalDate.now());
        }
    }

    private void setupTables() {
        filteredSalesList = new FilteredList<>(masterSalesList, p -> true);
        tvSales.setItems(filteredSalesList);

        colSaleId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().sale.getSale_id()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sale.getSale_date() != null ? c.getValue().sale.getSale_date().format(formatter) : ""));
        
        colCashier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cashierName));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName));
        colPaymentMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sale.getPayment_method()));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().sale.getTotal_amount()));
        
        colStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    SaleWrapper wrapper = getTableView().getItems().get(getIndex());
                    BigDecimal custDebt = wrapper.sale.getTotal_customer_debt() != null ? wrapper.sale.getTotal_customer_debt() : BigDecimal.ZERO;
                    BigDecimal insDebt = wrapper.sale.getTotal_insurance_debt() != null ? wrapper.sale.getTotal_insurance_debt() : BigDecimal.ZERO;

                    if (custDebt.compareTo(BigDecimal.ZERO) == 0 && insDebt.compareTo(BigDecimal.ZERO) == 0) {
                        setText("مكتملة");
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: CENTER;"); 
                    } else {
                        setText("ذمة معلقة");
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-alignment: CENTER;"); 
                    }
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("عرض");
            private final HBox actionPane = new HBox(viewBtn);

            {
                actionPane.setAlignment(Pos.CENTER);
                viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                
                viewBtn.setOnAction(event -> {
                    SaleWrapper wrapper = getTableView().getItems().get(getIndex());
                    
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/pharmacy/views/pos/SaleDetailsView.fxml"));
                        javafx.scene.Parent root = loader.load();
                        
                        SaleDetailsController controller = loader.getController();
                        // <--- تم تمرير returnService بنجاح هنا
                        controller.initData(wrapper.sale, wrapper.cashierName, saleService, userService, customerService, batchService, medicineService, insuranceService, returnService);
                        javafx.stage.Stage stage = new javafx.stage.Stage();
                        stage.setTitle("تفاصيل الفاتورة");
                        stage.setScene(new javafx.scene.Scene(root));
                        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); 
                        stage.showAndWait();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        AlertManager.showError("خطأ", "تعذر فتح نافذة التفاصيل.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionPane);
                }
            }
        });
    }

    @FXML
    public void loadInitialData() {
        txtSearch.clear();
        cbPaymentMethod.getSelectionModel().selectFirst();
        handleSearch();
    }

    @FXML
    private void handleSearch() {
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();
        LocalDate adjustedEnd = end.plusDays(1);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<Sale> dateFilteredSales = saleService.getSalesByDateRange(start, adjustedEnd);
                    List<SaleWrapper> wrappers = new ArrayList<>();

                    int currentShiftId = SessionManager.getInstance().getCurrentShift() != null ? 
                                         SessionManager.getInstance().getCurrentShift().getShift_id() : -1;
                    boolean isManager = SessionManager.getInstance().hasPermission("pos_view_all_sales");

                    for (Sale sale : dateFilteredSales) {
                        if (!isManager && sale.getShift_id() != currentShiftId) {
                            continue; 
                        }

                        String cashierName = currentUser.getFull_name();

                        String customerName = "زبون نقدي (عام)";
                        Integer custId = sale.getCustomer_id();
                        if (custId != null && custId > 0) {
                            Optional<LocalCustomer> optCust = customerService.getCustomerById(custId);
                            if (optCust.isPresent()) {
                                customerName = optCust.get().getName();
                            }
                        }

                        wrappers.add(new SaleWrapper(sale, cashierName, customerName));
                    }

                    Platform.runLater(() -> {
                        masterSalesList.setAll(wrappers);
                        applyLocalFilters();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> AlertManager.showError("خطأ", "حدث خطأ أثناء جلب الفواتير من قاعدة البيانات."));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void applyLocalFilters() {
        if (filteredSalesList == null) return;

        filteredSalesList.setPredicate(wrapper -> {
            String search = txtSearch.getText().toLowerCase().trim();
            String payment = cbPaymentMethod.getValue();

            boolean matchSearch = search.isEmpty() || 
                                  String.valueOf(wrapper.sale.getSale_id()).contains(search) ||
                                  wrapper.customerName.toLowerCase().contains(search);
            
            boolean matchPayment = payment == null || "الكل".equals(payment) || payment.equals(wrapper.sale.getPayment_method());

            boolean matchCashier = true;
            boolean isManager = SessionManager.getInstance().hasPermission("pos_view_all_sales");
            
            if (isManager) {
                List<String> selectedCashiers = new ArrayList<>();
                for (MenuItem item : mbCashier.getItems()) {
                    if (item instanceof CheckMenuItem && ((CheckMenuItem) item).isSelected()) {
                        selectedCashiers.add(item.getText());
                    }
                }
                if (!selectedCashiers.isEmpty()) {
                    matchCashier = selectedCashiers.contains(wrapper.cashierName);
                }
            }

            return matchSearch && matchPayment && matchCashier;
        });
    }

    public static class SaleWrapper {
        Sale sale;
        String cashierName;
        String customerName;

        public SaleWrapper(Sale sale, String cashierName, String customerName) {
            this.sale = sale;
            this.cashierName = cashierName;
            this.customerName = customerName;
        }
    }

    public static class SaleDetailWrapper {
        SaleDetail detail;
        String medicineName;
        String batchNumber;

        public SaleDetailWrapper(SaleDetail detail, String medicineName, String batchNumber) {
            this.detail = detail;
            this.medicineName = medicineName;
            this.batchNumber = batchNumber;
        }
    }
}