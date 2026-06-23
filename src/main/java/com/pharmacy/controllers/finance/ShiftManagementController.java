package com.pharmacy.controllers.finance;

import com.pharmacy.dao.impl.security.ShiftDAOImpl;
import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.models.security.Shift;
import com.pharmacy.models.security.User;
import com.pharmacy.security.SessionManager;
import com.pharmacy.services.impl.security.ShiftServiceImpl;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.services.interfaces.security.ShiftService;
import com.pharmacy.services.interfaces.security.UserService;
import com.pharmacy.utils.gui.AlertManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ShiftManagementController implements Initializable {

    @FXML private TextField txtSearchCashier;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private ComboBox<String> cbStatus;

    @FXML private TableView<ShiftWrapper> tvShifts;
    @FXML private TableColumn<ShiftWrapper, Integer> colIndex, colShiftId;
    @FXML private TableColumn<ShiftWrapper, String> colCashier, colStartTime, colStatus;
    
    // الأعمدة المالية الجديدة
    @FXML private TableColumn<ShiftWrapper, BigDecimal> colOpening, colExpected, colActual;
    @FXML private TableColumn<ShiftWrapper, Shift> colDifference; // نمرر الوردية كاملة لنحسب الفرق
    
    @FXML private TableColumn<ShiftWrapper, Void> colActions;

    private ShiftService shiftService;
    private UserService userService;
    
    private ObservableList<ShiftWrapper> masterList = FXCollections.observableArrayList();
    private FilteredList<ShiftWrapper> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initServices();
        setupFilters();
        setupTable();
        loadData();
    }

    private void initServices() {
        this.shiftService = new ShiftServiceImpl(new ShiftDAOImpl());
        this.userService = new UserServiceImpl(new UserDAOImpl());
    }

    private void setupFilters() {
        cbStatus.getItems().addAll("الكل", "مفتوحة (Open)", "مغلقة (Closed)");
        cbStatus.getSelectionModel().selectFirst();

        dpStartDate.setValue(LocalDate.now().minusDays(7)); 
        dpEndDate.setValue(LocalDate.now());

        txtSearchCashier.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupTable() {
        filteredList = new FilteredList<>(masterList, p -> true);
        tvShifts.setItems(filteredList);

        colIndex.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(tvShifts.getItems().indexOf(c.getValue()) + 1));
        colShiftId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().shift.getShift_id()));
        colCashier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cashierName));
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm");
        colStartTime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().shift.getStart_time() != null ? c.getValue().shift.getStart_time().format(dtf) : "---"
        ));

        // تلوين الحالة (مفتوحة / مغلقة)
        colStatus.setCellFactory(param -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle("");
                } else {
                    String status = getTableRow().getItem().shift.getStatus();
                    if ("Open".equalsIgnoreCase(status)) {
                        setText("مفتوحة"); setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setText("مغلقة"); setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        // --- ربط الأعمدة المالية ---
        colOpening.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().shift.getOpening_balance()));
        colExpected.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().shift.getExpected_closing_balance()));
        colActual.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().shift.getActual_closing_balance()));
        colDifference.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().shift));

        // تنسيق الأرقام
        colOpening.setCellFactory(c -> createMoneyCell());
        colExpected.setCellFactory(c -> createMoneyCell());
        colActual.setCellFactory(c -> createMoneyCell());

        // تلوين العجز والزيادة (أهم عمود للمدير)
        colDifference.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Shift shift, boolean empty) {
                super.updateItem(shift, empty);
                if (empty || shift == null) {
                    setText(null); setStyle("");
                } else {
                    if ("Open".equalsIgnoreCase(shift.getStatus())) {
                        setText("قيد العمل...");
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #95a5a6;");
                    } else {
                        BigDecimal expected = shift.getExpected_closing_balance() != null ? shift.getExpected_closing_balance() : BigDecimal.ZERO;
                        BigDecimal actual = shift.getActual_closing_balance() != null ? shift.getActual_closing_balance() : BigDecimal.ZERO;
                        BigDecimal diff = actual.subtract(expected);

                        if (diff.compareTo(BigDecimal.ZERO) == 0) {
                            setText("متطابق ✓");
                            setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-background-color: #e8f8f5;");
                        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                            setText(formatMoney(diff) + " (عجز)");
                            setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #e74c3c;");
                        } else {
                            setText("+" + formatMoney(diff) + " (زيادة)");
                            setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #f39c12;");
                        }
                    }
                }
            }
        });

        // زر عرض التفاصيل والجرد
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("تفاصيل الجرد");
            private final HBox actionPane = new HBox(viewBtn);

            {
                actionPane.setAlignment(Pos.CENTER);
                viewBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-cursor: hand;");
                
                viewBtn.setOnAction(event -> {
                    ShiftWrapper wrapper = getTableView().getItems().get(getIndex());
                    openShiftDetailsModal(wrapper.shift.getShift_id());
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionPane);
            }
        });
    }

    @FXML
    public void loadData() {
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        if (start == null || end == null) return;
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<Shift> shifts = shiftService.getShiftsByDateRange(
                            start.atStartOfDay(), 
                            end.plusDays(1).atStartOfDay(), 
                            1000, 0
                    );
                    
                    List<ShiftWrapper> wrappers = new ArrayList<>();
                    boolean isManager = SessionManager.getInstance().hasPermission("finance_view_all_shifts");
                    int currentUserId = SessionManager.getInstance().getCurrentUser().getUser_id();

                    for (Shift shift : shifts) {
                        if (!isManager && shift.getUser_id() != currentUserId) continue;

                        String cName = "غير معروف";
                        Optional<User> optUser = userService.getUserById(shift.getUser_id());
                        if (optUser.isPresent()) cName = optUser.get().getFull_name();

                        wrappers.add(new ShiftWrapper(shift, cName));
                    }

                    Platform.runLater(() -> {
                        masterList.setAll(wrappers);
                        applyFilters();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> AlertManager.showError("خطأ", "حدث خطأ أثناء جلب بيانات الورديات."));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void applyFilters() {
        if (filteredList == null) return;

        filteredList.setPredicate(wrapper -> {
            String search = txtSearchCashier.getText().toLowerCase().trim();
            String status = cbStatus.getValue();

            boolean matchSearch = search.isEmpty() || wrapper.cashierName.toLowerCase().contains(search);
            
            boolean matchStatus = true;
            if ("مفتوحة (Open)".equals(status)) matchStatus = "Open".equalsIgnoreCase(wrapper.shift.getStatus());
            else if ("مغلقة (Closed)".equals(status)) matchStatus = "Closed".equalsIgnoreCase(wrapper.shift.getStatus());

            return matchSearch && matchStatus;
        });
    }

    private void openShiftDetailsModal(int shiftId) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/pharmacy/views/finance/ShiftDetailsModalView.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // جلب الكنترولر وتمرير رقم الوردية إليه
            com.pharmacy.controllers.finance.shifts.ShiftDetailsModalController controller = loader.getController();
            controller.initData(shiftId);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("التفاصيل المالية للجرد");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // لكي لا يمكن الضغط على ما خلفها
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            AlertManager.showError("خطأ", "تعذر فتح نافذة التفاصيل المالية.");
        }
    }

    // دوال مساعدة للتنسيق المالي
    private TableCell<ShiftWrapper, BigDecimal> createMoneyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (getTableRow() != null && getTableRow().getItem() != null && "Open".equalsIgnoreCase(getTableRow().getItem().shift.getStatus()) && getTableColumn() != colOpening) {
                    setText("---"); // لا نعرض المتوقع والفعلي للوردية المفتوحة
                } else {
                    setText(formatMoney(item));
                }
            }
        };
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ل.س";
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return String.format("%,d ل.س", rounded.toBigInteger());
    }

    public static class ShiftWrapper {
        Shift shift;
        String cashierName;

        public ShiftWrapper(Shift shift, String cashierName) {
            this.shift = shift;
            this.cashierName = cashierName;
        }
    }
}