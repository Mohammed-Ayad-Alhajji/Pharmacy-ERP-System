package com.pharmacy.services.impl.purchasing;

import com.pharmacy.dao.interfaces.inventory.BatchDAO;
import com.pharmacy.dao.interfaces.inventory.MedicineDAO;
import com.pharmacy.dao.interfaces.purchasing.PurchaseDetailDAO;
import com.pharmacy.dao.interfaces.purchasing.SupplierReturnDAO;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.models.purchasing.PurchaseDetail;
import com.pharmacy.models.purchasing.SupplierReturn;
import com.pharmacy.services.interfaces.purchasing.SupplierReturnService;
import com.pharmacy.security.SessionManager;
import com.pharmacy.utils.DBConnectionManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class SupplierReturnServiceImpl implements SupplierReturnService {

    private final SupplierReturnDAO supplierReturnDAO;
    private final BatchDAO batchDAO;
    private final PurchaseDetailDAO purchaseDetailDAO;
    private final MedicineDAO medicineDAO;

    // حقن كافة الـ DAOs المطلوبة للتحقق من الفاتورة والمستودع ومعامل التحويل
    public SupplierReturnServiceImpl(SupplierReturnDAO supplierReturnDAO, BatchDAO batchDAO, 
                                     PurchaseDetailDAO purchaseDetailDAO, MedicineDAO medicineDAO) {
        this.supplierReturnDAO = supplierReturnDAO;
        this.batchDAO = batchDAO;
        this.purchaseDetailDAO = purchaseDetailDAO;
        this.medicineDAO = medicineDAO;
    }

    @Override
    public boolean processMultipleReturns(List<SupplierReturn> returns) {
        if (returns == null || returns.isEmpty()) return false;

        int currentUserId;
        try {
            currentUserId = SessionManager.getInstance().getCurrentUserId();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("يجب تسجيل الدخول لإتمام المرتجع.");
        }

        Connection conn = null;
        try {
            conn = DBConnectionManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            String updateBatchSql = "UPDATE Batches SET quantity = quantity - ? WHERE batch_id = ?";
            String insertReturnSql = "INSERT INTO Supplier_Returns (purchase_id, batch_id, user_id, quantity_returned, total_refund_value, return_status, return_date, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement updateBatchStmt = conn.prepareStatement(updateBatchSql);
                 PreparedStatement insertReturnStmt = conn.prepareStatement(insertReturnSql)) {

                for (SupplierReturn ret : returns) {
                    // 1. جلب تفاصيل الفاتورة الأصلية (للتأكد من الكمية المشتراة)
                    List<PurchaseDetail> details = purchaseDetailDAO.findByPurchaseId(ret.getPurchase_id());
                    PurchaseDetail originalDetail = details.stream()
                            .filter(d -> d.getBatch_id() == ret.getBatch_id()).findFirst()
                            .orElseThrow(() -> new SQLException("الطبخة غير موجودة في هذه الفاتورة."));

                    // 2. حساب الكميات التي تم إرجاعها مسبقاً (لمنع الإرجاع المزدوج)
                    int alreadyReturnedBoxes = supplierReturnDAO.findByPurchaseId(ret.getPurchase_id()).stream()
                            .filter(r -> r.getBatch_id() == ret.getBatch_id())
                            .mapToInt(SupplierReturn::getQuantity_returned).sum();

                    int maxReturnableBoxes = originalDetail.getQuantity_received() - alreadyReturnedBoxes;
                    if (ret.getQuantity_returned() > maxReturnableBoxes) {
                        throw new SQLException("الكمية المرتجعة (" + ret.getQuantity_returned() + ") تتجاوز الكمية المتبقية القابلة للإرجاع (" + maxReturnableBoxes + ").");
                    }

                    // 3. جلب الطبخة والدواء لمعرفة معامل التحويل (conversion_factor)
                    Batch batch = batchDAO.findById(ret.getBatch_id())
                            .orElseThrow(() -> new SQLException("الطبخة غير موجودة في المستودع."));
                    Medicine medicine = medicineDAO.findById(batch.getMed_id())
                            .orElseThrow(() -> new SQLException("الدواء غير موجود في النظام."));

                    // 4. تحويل العلب إلى ظروف لخصمها من المستودع بشكل صحيح
                    int unitsToDeduct = ret.getQuantity_returned() * medicine.getConversion_factor();

                    if (unitsToDeduct > batch.getQuantity()) {
                        throw new SQLException("الرصيد الفعلي بالظروف (" + batch.getQuantity() + ") لا يكفي لإرجاع هذه الكمية.");
                    }

                    // 5. الخصم بالظروف من جدول Batches
                    updateBatchStmt.setInt(1, unitsToDeduct);
                    updateBatchStmt.setInt(2, ret.getBatch_id());
                    updateBatchStmt.addBatch();

                    // 6. تسجيل المرتجع بالعلب في جدول Supplier_Returns
                    insertReturnStmt.setInt(1, ret.getPurchase_id());
                    insertReturnStmt.setInt(2, ret.getBatch_id());
                    insertReturnStmt.setInt(3, currentUserId);
                    insertReturnStmt.setInt(4, ret.getQuantity_returned());
                    insertReturnStmt.setBigDecimal(5, ret.getTotal_refund_value());
                    insertReturnStmt.setString(6, "Completed");
                    insertReturnStmt.setString(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    if (ret.getReason() != null) insertReturnStmt.setString(8, ret.getReason());
                    else insertReturnStmt.setNull(8, java.sql.Types.VARCHAR);
                    
                    insertReturnStmt.addBatch();
                }

                updateBatchStmt.executeBatch();
                insertReturnStmt.executeBatch();
                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("فشل إتمام المرتجع: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ... (باقي دوال override كما هي بدون تغيير: createReturn, getReturnById, الخ) ...
    @Override public Optional<SupplierReturn> createReturn(SupplierReturn sr) { return processMultipleReturns(List.of(sr)) ? Optional.of(sr) : Optional.empty(); }
    @Override public boolean updateReturn(SupplierReturn sr) { throw new UnsupportedOperationException("لا يمكن التعديل."); }
    @Override public boolean deleteReturn(int returnId) { throw new UnsupportedOperationException("لا يمكن الحذف."); }
    @Override public Optional<SupplierReturn> getReturnById(int returnId) { return supplierReturnDAO.findById(returnId); }
    @Override public List<SupplierReturn> getReturnsByPurchase(int purchaseId) { return supplierReturnDAO.findByPurchaseId(purchaseId); }
    @Override public List<SupplierReturn> getReturnsByBatch(int batchId) { return supplierReturnDAO.findByBatchId(batchId); }
    @Override public List<SupplierReturn> getReturnsByStatus(String status) { return supplierReturnDAO.findByStatus(status); }
    @Override public List<SupplierReturn> getReturnsByDateRange(LocalDate start, LocalDate end) { return supplierReturnDAO.findByDateRange(start, end); }
    @Override public List<SupplierReturn> getReturnsByUser(int userId) { return supplierReturnDAO.findByUserId(userId); }
}