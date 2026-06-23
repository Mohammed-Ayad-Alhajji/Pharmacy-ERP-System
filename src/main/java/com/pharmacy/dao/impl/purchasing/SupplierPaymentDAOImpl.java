// مسار الملف: src/main/java/com/pharmacy/dao/impl/purchasing/SupplierPaymentDAOImpl.java

package com.pharmacy.dao.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.SupplierPaymentDAO;
import com.pharmacy.models.purchasing.SupplierPayment;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SupplierPaymentDAOImpl implements SupplierPaymentDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<SupplierPayment> create(SupplierPayment entity) {
        // تم إضافة transaction_type في جملة الـ SQL
        String sql = "INSERT INTO Supplier_Payments (supplier_id, purchase_id, shift_id, amount_paid, receipt_number, payment_date, payment_method, transaction_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getSupplier_id());
            
            // معالجة الدفعة العامة أو المرتبطة بفاتورة
            if (entity.getPurchase_id() == null || entity.getPurchase_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getPurchase_id());
            }
            
            pstmt.setInt(3, entity.getShift_id());
            pstmt.setBigDecimal(4, entity.getAmount_paid());
            
            if (entity.getReceipt_number() != null) {
                pstmt.setString(5, entity.getReceipt_number());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            if (entity.getPayment_date() != null) {
                pstmt.setString(6, entity.getPayment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            if (entity.getPayment_method() != null) {
                pstmt.setString(7, entity.getPayment_method());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }

            // --- الإضافة الجديدة: تعيين نوع الحركة (صرف أم استرداد) ---
            if (entity.getTransaction_type() != null) {
                pstmt.setString(8, entity.getTransaction_type());
            } else {
                pstmt.setString(8, "Payment"); // القيمة الافتراضية للسلامة
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setPayment_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج الدفعة المالية. تحقق من قيد (amount_paid > 0.0) أو صحة المعرفات الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<SupplierPayment> findById(Integer id) {
        String sql = "SELECT * FROM Supplier_Payments WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSupplierPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعة المالية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<SupplierPayment> findAll() {
        List<SupplierPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Payments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                payments.add(mapRowToSupplierPayment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة الدفعات المالية: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<SupplierPayment> findAll(int limit, int offset) {
        List<SupplierPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Payments LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToSupplierPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الدفعات (Pagination): " + e.getMessage());
        }
        return payments;
    }

    @Override
    public boolean update(SupplierPayment entity) {
        String sql = "UPDATE Supplier_Payments SET supplier_id = ?, purchase_id = ?, shift_id = ?, amount_paid = ?, receipt_number = ?, payment_date = ?, payment_method = ? WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getSupplier_id());
            
            if (entity.getPurchase_id() == null || entity.getPurchase_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getPurchase_id());
            }
            
            pstmt.setInt(3, entity.getShift_id());
            pstmt.setBigDecimal(4, entity.getAmount_paid());
            
            if (entity.getReceipt_number() != null) {
                pstmt.setString(5, entity.getReceipt_number());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            if (entity.getPayment_date() != null) {
                pstmt.setString(6, entity.getPayment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            if (entity.getPayment_method() != null) {
                pstmt.setString(7, entity.getPayment_method());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            pstmt.setInt(8, entity.getPayment_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث الدفعة المالية. تأكد من صحة القيود المحاسبية والمعرفات الأجنبية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف الدفعات المالية الموثقة");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Supplier_Payments WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الدفعة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Supplier_Payments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الدفعات: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<SupplierPayment> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Supplier_Payments (supplier_id, purchase_id, shift_id, amount_paid, receipt_number, payment_date, payment_method) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (SupplierPayment entity : entities) {
                    pstmt.setInt(1, entity.getSupplier_id());
                    
                    if (entity.getPurchase_id() == null || entity.getPurchase_id() == 0) {
                        pstmt.setNull(2, Types.INTEGER);
                    } else {
                        pstmt.setInt(2, entity.getPurchase_id());
                    }
                    
                    pstmt.setInt(3, entity.getShift_id());
                    pstmt.setBigDecimal(4, entity.getAmount_paid());
                    
                    if (entity.getReceipt_number() != null) {
                        pstmt.setString(5, entity.getReceipt_number());
                    } else {
                        pstmt.setNull(5, Types.VARCHAR);
                    }
                    
                    if (entity.getPayment_date() != null) {
                        pstmt.setString(6, entity.getPayment_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    if (entity.getPayment_method() != null) {
                        pstmt.setString(7, entity.getPayment_method());
                    } else {
                        pstmt.setNull(7, Types.VARCHAR);
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للدفعات المالية، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private SupplierPayment mapRowToSupplierPayment(ResultSet rs) throws SQLException {
        SupplierPayment payment = new SupplierPayment();
        payment.setPayment_id(rs.getInt("payment_id"));
        payment.setSupplier_id(rs.getInt("supplier_id"));
        
        int purchaseId = rs.getInt("purchase_id");
        if (!rs.wasNull()) {
            payment.setPurchase_id(purchaseId);
        } else {
            payment.setPurchase_id(null);
        }
        
        payment.setShift_id(rs.getInt("shift_id"));
        payment.setAmount_paid(rs.getBigDecimal("amount_paid"));
        payment.setReceipt_number(rs.getString("receipt_number"));
        
        String dateStr = rs.getString("payment_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            payment.setPayment_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        payment.setPayment_method(rs.getString("payment_method"));
        
        // --- الإضافة الجديدة: قراءة نوع الحركة من قاعدة البيانات ---
        String tType = rs.getString("transaction_type");
        if (tType != null && !tType.trim().isEmpty()) {
            payment.setTransaction_type(tType);
        } else {
            // للتعامل مع البيانات القديمة التي تم إدخالها قبل إضافة العمود
            payment.setTransaction_type("Payment"); 
        }
        
        return payment;
    }

    @Override
    public List<SupplierPayment> findBySupplierId(int supplierId) {
        List<SupplierPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Payments WHERE supplier_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, supplierId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToSupplierPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات الخاصة بالمورد: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<SupplierPayment> findByPurchaseId(int purchaseId) {
        List<SupplierPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Payments WHERE purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, purchaseId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToSupplierPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات المرتبطة بالفاتورة: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<SupplierPayment> findByShiftId(int shiftId) {
        List<SupplierPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Payments WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shiftId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToSupplierPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن دفعات الوردية: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<SupplierPayment> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<SupplierPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Payments WHERE payment_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endDateTime = endDate.atTime(23, 59, 59).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToSupplierPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات ضمن النطاق الزمني: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<SupplierPayment> findByPaymentMethod(String method) {
        List<SupplierPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Payments WHERE payment_method = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, method);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToSupplierPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات حسب طريقة الدفع: " + e.getMessage());
        }
        return payments;
    }
}