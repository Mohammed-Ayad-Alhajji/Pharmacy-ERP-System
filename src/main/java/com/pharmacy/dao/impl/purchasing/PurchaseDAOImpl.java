// مسار الملف: src/main/java/com/pharmacy/dao/impl/purchasing/PurchaseDAOImpl.java

package com.pharmacy.dao.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.PurchaseDAO;
import com.pharmacy.models.purchasing.Purchase;
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

public class PurchaseDAOImpl implements PurchaseDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String BASE_QUERY = 
        "SELECT p.*, s.name as supplier_name FROM Purchases p " +
        "JOIN Suppliers s ON p.supplier_id = s.supplier_id";

    @Override
    public Optional<Purchase> create(Purchase entity) {
        String query = "INSERT INTO Purchases (supplier_id, shift_id, purchase_date, total_cost, payment_status, supplier_invoice_number) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, entity.getSupplier_id());
            pstmt.setInt(2, entity.getShift_id()); 
            pstmt.setString(3, entity.getPurchase_date().format(DATE_TIME_FORMATTER));
            pstmt.setBigDecimal(4, entity.getTotal_cost());
            pstmt.setString(5, entity.getPayment_status());
            pstmt.setString(6, entity.getSupplier_invoice_number());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setPurchase_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating Purchase: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Purchase> findById(Integer id) {
        String sql = BASE_QUERY + " WHERE p.purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPurchase(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الفاتورة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Purchase> findAll() {
        List<Purchase> purchases = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(BASE_QUERY);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                purchases.add(mapRowToPurchase(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ] فشل جلب الفواتير: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<Purchase> findAll(int limit, int offset) {
        List<Purchase> purchases = new ArrayList<>();
        String sql = BASE_QUERY + " LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    purchases.add(mapRowToPurchase(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الفواتير (Pagination): " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public boolean update(Purchase entity) {
        String query = "UPDATE Purchases SET supplier_id = ?, shift_id = ?, purchase_date = ?, total_cost = ?, payment_status = ?, supplier_invoice_number = ? WHERE purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, entity.getSupplier_id());
            pstmt.setInt(2, entity.getShift_id()); 
            pstmt.setString(3, entity.getPurchase_date().format(DATE_TIME_FORMATTER));
            pstmt.setBigDecimal(4, entity.getTotal_cost());
            pstmt.setString(5, entity.getPayment_status());
            pstmt.setString(6, entity.getSupplier_invoice_number());
            pstmt.setInt(7, entity.getPurchase_id());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating Purchase: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // الدالة الجديدة: لتحديث حالة الدفع فقط
    // ==========================================
    @Override
    public boolean updatePaymentStatus(int purchaseId, String status) {
        String query = "UPDATE Purchases SET payment_status = ? WHERE purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setString(1, status);
            pstmt.setInt(2, purchaseId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating payment status: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Purchases WHERE purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[إجراء محظور] يُمنع حذف فاتورة مرتبطة بتفاصيل أو دفعات مالية. العملية أُلغيت بسبب قيود (ON DELETE RESTRICT): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Purchases WHERE purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الفاتورة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Purchases";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الفواتير: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Purchase> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Purchases (supplier_id, shift_id, purchase_date, total_cost, payment_status, supplier_invoice_number) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Purchase entity : entities) {
                    pstmt.setInt(1, entity.getSupplier_id());
                    pstmt.setInt(2, entity.getShift_id());
                    
                    if (entity.getPurchase_date() != null) {
                        pstmt.setString(3, entity.getPurchase_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(3, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    pstmt.setBigDecimal(4, entity.getTotal_cost());
                    pstmt.setString(5, entity.getPayment_status() != null ? entity.getPayment_status() : "Unpaid");
                    pstmt.setString(6, entity.getSupplier_invoice_number());
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للفواتير، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ الفواتير المجمعة: " + e.getMessage());
            return false;
        }
    }

    private Purchase mapRowToPurchase(ResultSet rs) throws SQLException {
        Purchase purchase = new Purchase();
        purchase.setPurchase_id(rs.getInt("purchase_id"));
        purchase.setSupplier_id(rs.getInt("supplier_id"));
        purchase.setSupplier_name(rs.getString("supplier_name")); 
        purchase.setShift_id(rs.getInt("shift_id")); 
        
        String dateStr = rs.getString("purchase_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            purchase.setPurchase_date(LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        purchase.setTotal_cost(rs.getBigDecimal("total_cost"));
        purchase.setPayment_status(rs.getString("payment_status"));
        purchase.setSupplier_invoice_number(rs.getString("supplier_invoice_number"));
        return purchase;
    }

    @Override
    public List<Purchase> findBySupplierId(int supplierId) {
        List<Purchase> purchases = new ArrayList<>();
        String sql = BASE_QUERY + " WHERE p.supplier_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, supplierId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    purchases.add(mapRowToPurchase(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن فواتير المورد: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<Purchase> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Purchase> purchases = new ArrayList<>();
        String sql = BASE_QUERY + " WHERE p.purchase_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endDateTime = endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    purchases.add(mapRowToPurchase(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الفواتير ضمن النطاق الزمني: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<Purchase> findByPaymentStatus(String status) {
        List<Purchase> purchases = new ArrayList<>();
        String sql = BASE_QUERY + " WHERE p.payment_status = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    purchases.add(mapRowToPurchase(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الفواتير بحالة الدفع: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<Purchase> findByShiftId(int shiftId) {
        List<Purchase> purchases = new ArrayList<>();
        String sql = BASE_QUERY + " WHERE p.shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shiftId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    purchases.add(mapRowToPurchase(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن فواتير الوردية: " + e.getMessage());
        }
        return purchases;
    }
}