package com.pharmacy.controllers.inventory;

import com.pharmacy.dao.impl.inventory.BatchDAOImpl;
import com.pharmacy.dao.impl.inventory.DisposalDAOImpl;
import com.pharmacy.dao.impl.inventory.InventoryAdjustmentDAOImpl;
import com.pharmacy.dao.impl.inventory.MedicineDAOImpl;
import com.pharmacy.dao.impl.security.UserDAOImpl;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Disposal;
import com.pharmacy.models.inventory.InventoryAdjustment;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.security.User;
import com.pharmacy.services.impl.inventory.BatchServiceImpl;
import com.pharmacy.services.impl.inventory.DisposalServiceImpl;
import com.pharmacy.services.impl.inventory.InventoryAdjustmentServiceImpl;
import com.pharmacy.services.impl.inventory.MedicineServiceImpl;
import com.pharmacy.services.impl.security.UserServiceImpl;
import com.pharmacy.utils.gui.ViewManager;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryAdjustmentMainController {

    // === التسويات ===
    @FXML private DatePicker adjStartDate, adjEndDate;
    @FXML private ComboBox<User> adjUserCombo;
    @FXML private TextField adjSearchField;
    @FXML private TableView<AdjWrapper> adjTable;
    @FXML private TableColumn<AdjWrapper, Integer> adjIdCol, adjSysQtyCol, adjActualQtyCol, adjDiffCol;
    @FXML private TableColumn<AdjWrapper, String> adjDateCol, adjUserCol, adjMedCol, adjBatchCol, adjNotesCol;
    @FXML private ComboBox<Integer> adjRowsCombo;
    @FXML private Pagination adjPagination;

    // === الإتلافات ===
    @FXML private DatePicker dispStartDate, dispEndDate;
    @FXML private ComboBox<User> dispUserCombo;
    @FXML private TextField dispSearchField;
    @FXML private TableView<DispWrapper> dispTable;
    @FXML private TableColumn<DispWrapper, Integer> dispIdCol, dispQtyCol;
    @FXML private TableColumn<DispWrapper, BigDecimal> dispCostCol, dispCompCol, dispLossCol;
    @FXML private TableColumn<DispWrapper, String> dispDateCol, dispUserCol, dispMedCol, dispBatchCol, dispReasonCol;
    @FXML private ComboBox<Integer> dispRowsCombo;
    @FXML private Pagination dispPagination;

    private InventoryAdjustmentServiceImpl adjService;
    private DisposalServiceImpl dispService;
    private UserServiceImpl userService;
    private BatchServiceImpl batchService;
    private MedicineServiceImpl medService;

    private ObservableList<AdjWrapper> masterAdjList = FXCollections.observableArrayList();
    private FilteredList<AdjWrapper> filteredAdjList;

    private ObservableList<DispWrapper> masterDispList = FXCollections.observableArrayList();
    private FilteredList<DispWrapper> filteredDispList;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // نظام التخزين المؤقت (Caching) لتسريع الأداء
    private Map<Integer, User> userCache = new HashMap<>();
    private Map<Integer, Batch> batchCache = new HashMap<>();
    private Map<Integer, Medicine> medCache = new HashMap<>();

    @FXML
    public void initialize() {
        initServices();
        setupUserCombos();
        setupDates();
        
        setupAdjTable();
        setupDispTable();
        
        setupPaginationControls();
        setupListeners();

        loadAllData();
    }

    private void initServices() {
        adjService = new InventoryAdjustmentServiceImpl(new InventoryAdjustmentDAOImpl());
        dispService = new DisposalServiceImpl(new DisposalDAOImpl());
        userService = new UserServiceImpl(new UserDAOImpl());
        batchService = new BatchServiceImpl(new BatchDAOImpl());
        medService = new MedicineServiceImpl(new MedicineDAOImpl());
    }

    private void setupDates() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        adjStartDate.setValue(start); adjEndDate.setValue(end);
        dispStartDate.setValue(start); dispEndDate.setValue(end);
    }

    private void setupUserCombos() {
        // استخدام الدالة الموجودة في UserServiceImpl لجلب المستخدمين النشطين
        List<User> users = userService.getUsers(false, 10000, 0);
        ObservableList<User> userList = FXCollections.observableArrayList(users);
        
        StringConverter<User> userConverter = new StringConverter<>() {
            @Override public String toString(User u) { return u != null ? u.getFull_name() : "كل المستخدمين"; }
            @Override public User fromString(String s) { return null; }
        };

        adjUserCombo.setItems(userList); adjUserCombo.setConverter(userConverter);
        dispUserCombo.setItems(userList); dispUserCombo.setConverter(userConverter);
    }

    // ==========================================
    // إعداد الجداول (Tables)
    // ==========================================
    private void setupAdjTable() {
        adjIdCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().adj.getAdjustment_id()));
        adjDateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().adj.getAdjustment_date().format(dtf)));
        adjUserCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().userName));
        adjMedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medName));
        adjBatchCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().batchNum));
        adjSysQtyCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().adj.getSystem_quantity()));
        adjActualQtyCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().adj.getActual_quantity()));
        adjNotesCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().adj.getNotes()));

        adjDiffCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().adj.getDifference()));
        adjDiffCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); } 
                else {
                    setText(String.valueOf(item));
                    if (item < 0) setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); 
                    else if (item > 0) setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); 
                    else setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupDispTable() {
        dispIdCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().disp.getDisposal_id()));
        dispDateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().disp.getDisposal_date().format(dtf)));
        dispUserCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().userName));
        dispMedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().medName));
        dispBatchCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().batchNum));
        dispQtyCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().disp.getQuantity_disposed()));
        dispCostCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().disp.getTotal_cost()));
        dispCompCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().disp.getSupplier_compensation_amount()));
        dispLossCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().disp.getPharmacy_loss_amount()));
        dispReasonCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().disp.getReason()));
    }

    // ==========================================
    // جلب البيانات مع Caching (السرعة القصوى)
    // ==========================================
    private void loadAllData() {
        // 1. تحميل التسويات
        List<InventoryAdjustment> adjs = adjService.findAll(); // يفضل استبدالها بجلب حسب التاريخ
        masterAdjList.clear();
        for (InventoryAdjustment a : adjs) {
            String uName = getUserName(a.getUser_id()); // يجب إضافة حقل user_id في المودل أو تجاوزه إذا لم يكن موجوداً
            String[] medInfo = getMedInfo(a.getBatch_id());
            masterAdjList.add(new AdjWrapper(a, uName, medInfo[0], medInfo[1]));
        }

        // 2. تحميل الإتلافات
        List<Disposal> disps = new DisposalDAOImpl().findAll(); 
        masterDispList.clear();
        for (Disposal d : disps) {
            String uName = getUserName(d.getUser_id());
            String[] medInfo = getMedInfo(d.getBatch_id());
            masterDispList.add(new DispWrapper(d, uName, medInfo[0], medInfo[1]));
        }

        applyAdjFilters();
        applyDispFilters();
    }

    // دوال المساعدة للـ Caching
    private String getUserName(int userId) {
        if (userId <= 0) return "مجهول";
        if (!userCache.containsKey(userId)) userService.getUserById(userId).ifPresent(u -> userCache.put(userId, u));
        return userCache.containsKey(userId) ? userCache.get(userId).getFull_name() : "مجهول";
    }

    private String[] getMedInfo(int batchId) {
        if (!batchCache.containsKey(batchId)) {
            // تصحيح: التعامل مع Optional<Batch>
            batchService.getBatchById(batchId).ifPresent(b -> batchCache.put(batchId, b));
        }
        
        Batch b = batchCache.get(batchId);
        if (b == null) return new String[]{"غير معروف", String.valueOf(batchId)};

        if (!medCache.containsKey(b.getMed_id())) {
            // افتراض أن getMedicineById تعود أيضاً بـ Optional<Medicine>
            medService.getMedicineById(b.getMed_id()).ifPresent(m -> medCache.put(b.getMed_id(), m));
        }
        
        Medicine m = medCache.get(b.getMed_id());
        String medName = m != null ? m.getBrand_name() : "غير معروف";
        return new String[]{medName, b.getBatch_number()};
    }

    // ==========================================
    // الفلاتر ونظام الـ Pagination
    // ==========================================
    private void setupListeners() {
        filteredAdjList = new FilteredList<>(masterAdjList, p -> true);
        filteredDispList = new FilteredList<>(masterDispList, p -> true);

        // مستمعات التسويات
        adjStartDate.valueProperty().addListener((o, old, newVal) -> applyAdjFilters());
        adjEndDate.valueProperty().addListener((o, old, newVal) -> applyAdjFilters());
        adjUserCombo.valueProperty().addListener((o, old, newVal) -> applyAdjFilters());
        adjSearchField.textProperty().addListener((o, old, newVal) -> applyAdjFilters());

        // مستمعات الإتلافات
        dispStartDate.valueProperty().addListener((o, old, newVal) -> applyDispFilters());
        dispEndDate.valueProperty().addListener((o, old, newVal) -> applyDispFilters());
        dispUserCombo.valueProperty().addListener((o, old, newVal) -> applyDispFilters());
        dispSearchField.textProperty().addListener((o, old, newVal) -> applyDispFilters());
    }

    private void applyAdjFilters() {
        filteredAdjList.setPredicate(w -> {
            LocalDate date = w.adj.getAdjustment_date() != null ? w.adj.getAdjustment_date().toLocalDate() : null;
            if (date != null && adjStartDate.getValue() != null && date.isBefore(adjStartDate.getValue())) return false;
            if (date != null && adjEndDate.getValue() != null && date.isAfter(adjEndDate.getValue())) return false;
            
            if (adjUserCombo.getValue() != null && !w.userName.equals(adjUserCombo.getValue().getFull_name())) return false;
            
            String search = adjSearchField.getText().toLowerCase();
            if (!search.isEmpty() && !w.medName.toLowerCase().contains(search) && (w.adj.getNotes() == null || !w.adj.getNotes().toLowerCase().contains(search))) return false;
            
            return true;
        });
        updateAdjPagination();
    }

    private void applyDispFilters() {
        filteredDispList.setPredicate(w -> {
            LocalDate date = w.disp.getDisposal_date() != null ? w.disp.getDisposal_date().toLocalDate() : null;
            if (date != null && dispStartDate.getValue() != null && date.isBefore(dispStartDate.getValue())) return false;
            if (date != null && dispEndDate.getValue() != null && date.isAfter(dispEndDate.getValue())) return false;
            
            if (dispUserCombo.getValue() != null && !w.userName.equals(dispUserCombo.getValue().getFull_name())) return false;
            
            String search = dispSearchField.getText().toLowerCase();
            if (!search.isEmpty() && !w.medName.toLowerCase().contains(search) && (w.disp.getReason() == null || !w.disp.getReason().toLowerCase().contains(search))) return false;
            
            return true;
        });
        updateDispPagination();
    }

    private void setupPaginationControls() {
        ObservableList<Integer> rows = FXCollections.observableArrayList(10, 20, 50, 100);
        
        adjRowsCombo.setItems(rows); adjRowsCombo.setValue(20);
        adjRowsCombo.valueProperty().addListener((o, old, newVal) -> updateAdjPagination());
        adjPagination.currentPageIndexProperty().addListener((o, old, newIndex) -> updateAdjTableData(newIndex.intValue()));

        dispRowsCombo.setItems(rows); dispRowsCombo.setValue(20);
        dispRowsCombo.valueProperty().addListener((o, old, newVal) -> updateDispPagination());
        dispPagination.currentPageIndexProperty().addListener((o, old, newIndex) -> updateDispTableData(newIndex.intValue()));
    }

    private void updateAdjPagination() {
        int rows = adjRowsCombo.getValue();
        int pageCount = (int) Math.ceil((double) filteredAdjList.size() / rows);
        adjPagination.setPageCount(pageCount == 0 ? 1 : pageCount);
        updateAdjTableData(adjPagination.getCurrentPageIndex());
    }

    private void updateAdjTableData(int pageIndex) {
        int rows = adjRowsCombo.getValue();
        int from = pageIndex * rows;
        int to = Math.min(from + rows, filteredAdjList.size());
        adjTable.setItems(FXCollections.observableArrayList(filteredAdjList.subList(from, to)));
    }

    private void updateDispPagination() {
        int rows = dispRowsCombo.getValue();
        int pageCount = (int) Math.ceil((double) filteredDispList.size() / rows);
        dispPagination.setPageCount(pageCount == 0 ? 1 : pageCount);
        updateDispTableData(dispPagination.getCurrentPageIndex());
    }

    private void updateDispTableData(int pageIndex) {
        int rows = dispRowsCombo.getValue();
        int from = pageIndex * rows;
        int to = Math.min(from + rows, filteredDispList.size());
        dispTable.setItems(FXCollections.observableArrayList(filteredDispList.subList(from, to)));
    }

    // ==========================================
    // الأزرار
    // ==========================================
    @FXML private void clearAdjFilters(ActionEvent event) {
        setupDates(); adjUserCombo.setValue(null); adjSearchField.clear(); applyAdjFilters();
    }

    @FXML private void clearDispFilters(ActionEvent event) {
        setupDates(); dispUserCombo.setValue(null); dispSearchField.clear(); applyDispFilters();
    }

    @FXML private void handleNewAdjustment(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/inventory/AddAdjustmentDialog.fxml", "إجراء تسوية جرد جديدة", null);
        loadAllData();
    }

    @FXML private void handleNewDisposal(ActionEvent event) {
        ViewManager.getInstance().showModalWithData("/com/pharmacy/views/inventory/AddDisposalDialog.fxml", "تسجيل إتلاف أدوية", null);
        loadAllData();
    }

    // ==========================================
    // كلاسات التغليف (Wrappers)
    // ==========================================
    public static class AdjWrapper {
        public InventoryAdjustment adj;
        public String userName, medName, batchNum;
        public AdjWrapper(InventoryAdjustment a, String u, String m, String b) { this.adj = a; this.userName = u; this.medName = m; this.batchNum = b; }
    }

    public static class DispWrapper {
        public Disposal disp;
        public String userName, medName, batchNum;
        public DispWrapper(Disposal d, String u, String m, String b) { this.disp = d; this.userName = u; this.medName = m; this.batchNum = b; }
    }
}