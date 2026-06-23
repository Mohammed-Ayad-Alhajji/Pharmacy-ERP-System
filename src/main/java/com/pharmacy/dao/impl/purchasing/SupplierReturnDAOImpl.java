// مسار الملف: src/main/java/com/pharmacy/dao/impl/purchasing/SupplierReturnDAOImpl.java

package com.pharmacy.dao.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.SupplierReturnDAO;
import com.pharmacy.models.purchasing.SupplierReturn;
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

public class SupplierReturnDAOImpl implements SupplierReturnDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<SupplierReturn> create(SupplierReturn entity) {
        String sql = "INSERT INTO Supplier_Returns (purchase_id, batch_id, user_id, quantity_returned, total_refund_value, return_status, return_date, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getPurchase_id());
            pstmt.setInt(2, entity.getBatch_id());
            pstmt.setInt(3, entity.getUser_id());
            pstmt.setInt(4, entity.getQuantity_returned());
            pstmt.setBigDecimal(5, entity.getTotal_refund_value());
            
            if (entity.getReturn_status() != null) {
                pstmt.setString(6, entity.getReturn_status());
            } else {
                pstmt.setString(6, "Pending");
            }
            
            if (entity.getReturn_date() != null) {
                pstmt.setString(7, entity.getReturn_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            if (entity.getReason() != null) {
                pstmt.setString(8, entity.getReason());
            } else {
                pstmt.setNull(8, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setSup_return_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج مرتجع المورد. تأكد من قيود CHECK (الكمية > 0، القيمة >= 0) والمفاتيح الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<SupplierReturn> findById(Integer id) {
        String sql = "SELECT * FROM Supplier_Returns WHERE sup_return_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSupplierReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجل المرتجع: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<SupplierReturn> findAll() {
        List<SupplierReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Returns";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                returns.add(mapRowToSupplierReturn(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة المرتجعات: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<SupplierReturn> findAll(int limit, int offset) {
        List<SupplierReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Returns LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToSupplierReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة المرتجعات (Pagination): " + e.getMessage());
        }
        return returns;
    }

    @Override
    public boolean update(SupplierReturn entity) {
        String sql = "UPDATE Supplier_Returns SET purchase_id = ?, batch_id = ?, user_id = ?, quantity_returned = ?, total_refund_value = ?, return_status = ?, return_date = ?, reason = ? WHERE sup_return_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getPurchase_id());
            pstmt.setInt(2, entity.getBatch_id());
            pstmt.setInt(3, entity.getUser_id());
            pstmt.setInt(4, entity.getQuantity_returned());
            pstmt.setBigDecimal(5, entity.getTotal_refund_value());
            pstmt.setString(6, entity.getReturn_status());
            
            if (entity.getReturn_date() != null) {
                pstmt.setString(7, entity.getReturn_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            if (entity.getReason() != null) {
                pstmt.setString(8, entity.getReason());
            } else {
                pstmt.setNull(8, Types.VARCHAR);
            }
            
            pstmt.setInt(9, entity.getSup_return_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث المرتجع. تحقق من القيود والمفاتيح الأجنبية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف سجلات مرتجعات الموردين الموثقة");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Supplier_Returns WHERE sup_return_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود المرتجع: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Supplier_Returns";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي المرتجعات: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<SupplierReturn> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Supplier_Returns (purchase_id, batch_id, user_id, quantity_returned, total_refund_value, return_status, return_date, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (SupplierReturn entity : entities) {
                    pstmt.setInt(1, entity.getPurchase_id());
                    pstmt.setInt(2, entity.getBatch_id());
                    pstmt.setInt(3, entity.getUser_id());
                    pstmt.setInt(4, entity.getQuantity_returned());
                    pstmt.setBigDecimal(5, entity.getTotal_refund_value());
                    
                    if (entity.getReturn_status() != null) {
                        pstmt.setString(6, entity.getReturn_status());
                    } else {
                        pstmt.setString(6, "Pending");
                    }
                    
                    if (entity.getReturn_date() != null) {
                        pstmt.setString(7, entity.getReturn_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    if (entity.getReason() != null) {
                        pstmt.setString(8, entity.getReason());
                    } else {
                        pstmt.setNull(8, Types.VARCHAR);
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للمرتجعات، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ المرتجعات المجمعة: " + e.getMessage());
            return false;
        }
    }

    private SupplierReturn mapRowToSupplierReturn(ResultSet rs) throws SQLException {
        SupplierReturn returnEntity = new SupplierReturn();
        returnEntity.setSup_return_id(rs.getInt("sup_return_id"));
        returnEntity.setPurchase_id(rs.getInt("purchase_id"));
        returnEntity.setBatch_id(rs.getInt("batch_id"));
        returnEntity.setUser_id(rs.getInt("user_id"));
        returnEntity.setQuantity_returned(rs.getInt("quantity_returned"));
        returnEntity.setTotal_refund_value(rs.getBigDecimal("total_refund_value"));
        returnEntity.setReturn_status(rs.getString("return_status"));
        
        String dateStr = rs.getString("return_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            returnEntity.setReturn_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        returnEntity.setReason(rs.getString("reason"));
        return returnEntity;
    }

    @Override
    public List<SupplierReturn> findByPurchaseId(int purchaseId) {
        List<SupplierReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Returns WHERE purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, purchaseId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToSupplierReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مرتجعات الفاتورة: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<SupplierReturn> findByBatchId(int batchId) {
        List<SupplierReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Returns WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, batchId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToSupplierReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مرتجعات التشغيلة: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<SupplierReturn> findByStatus(String status) {
        List<SupplierReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Returns WHERE return_status = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToSupplierReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المرتجعات حسب الحالة: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<SupplierReturn> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<SupplierReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Returns WHERE return_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endDateTime = endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToSupplierReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المرتجعات ضمن النطاق الزمني: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<SupplierReturn> findByUserId(int userId) {
        List<SupplierReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Returns WHERE user_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToSupplierReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مرتجعات المستخدم: " + e.getMessage());
        }
        return returns;
    }
}