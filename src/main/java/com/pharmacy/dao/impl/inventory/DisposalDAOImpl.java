// مسار الملف: src/main/java/com/pharmacy/dao/impl/inventory/DisposalDAOImpl.java

package com.pharmacy.dao.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.DisposalDAO;
import com.pharmacy.models.inventory.Disposal;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DisposalDAOImpl implements DisposalDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<Disposal> create(Disposal entity) {
        String sql = "INSERT INTO Disposals (batch_id, user_id, quantity_disposed, total_cost, pharmacy_loss_amount, supplier_compensation_amount, disposal_date, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getBatch_id());
            pstmt.setInt(2, entity.getUser_id());
            pstmt.setInt(3, entity.getQuantity_disposed());
            pstmt.setBigDecimal(4, entity.getTotal_cost());
            pstmt.setBigDecimal(5, entity.getPharmacy_loss_amount());
            pstmt.setBigDecimal(6, entity.getSupplier_compensation_amount());
            
            if (entity.getDisposal_date() != null) {
                pstmt.setString(7, entity.getDisposal_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            pstmt.setString(8, entity.getReason());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setDisposal_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الإدراج. تحقق من القيد المحاسبي (total_cost = pharmacy_loss_amount + supplier_compensation_amount) أو الكمية السالبة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Disposal> findById(Integer id) {
        String sql = "SELECT * FROM Disposals WHERE disposal_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDisposal(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجل الإتلاف: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Disposal> findAll() {
        List<Disposal> disposals = new ArrayList<>();
        String sql = "SELECT * FROM Disposals";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                disposals.add(mapRowToDisposal(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة سجلات الإتلاف: " + e.getMessage());
        }
        return disposals;
    }

    @Override
    public List<Disposal> findAll(int limit, int offset) {
        List<Disposal> disposals = new ArrayList<>();
        String sql = "SELECT * FROM Disposals LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    disposals.add(mapRowToDisposal(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب سجلات الإتلاف (Pagination): " + e.getMessage());
        }
        return disposals;
    }

    @Override
    public boolean update(Disposal entity) {
        String sql = "UPDATE Disposals SET batch_id = ?, user_id = ?, quantity_disposed = ?, total_cost = ?, pharmacy_loss_amount = ?, supplier_compensation_amount = ?, disposal_date = ?, reason = ? WHERE disposal_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getBatch_id());
            pstmt.setInt(2, entity.getUser_id());
            pstmt.setInt(3, entity.getQuantity_disposed());
            pstmt.setBigDecimal(4, entity.getTotal_cost());
            pstmt.setBigDecimal(5, entity.getPharmacy_loss_amount());
            pstmt.setBigDecimal(6, entity.getSupplier_compensation_amount());
            
            if (entity.getDisposal_date() != null) {
                pstmt.setString(7, entity.getDisposal_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(7, java.sql.Types.VARCHAR);
            }
            
            pstmt.setString(8, entity.getReason());
            pstmt.setInt(9, entity.getDisposal_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحديث. تحقق من المعادلة المحاسبية للإتلاف أو القيود الأخرى: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        // حظر الحذف الفعلي تنفيذاً للقيد الرقابي والمحاسبي الصارم
        System.err.println("[تحذير أمني] يُمنع حذف سجلات الإتلاف لأسباب رقابية ومحاسبية. العملية أُلغيت.");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Disposals WHERE disposal_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود السجل: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Disposals";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي سجلات الإتلاف: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Disposal> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Disposals (batch_id, user_id, quantity_disposed, total_cost, pharmacy_loss_amount, supplier_compensation_amount, disposal_date, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Disposal entity : entities) {
                    pstmt.setInt(1, entity.getBatch_id());
                    pstmt.setInt(2, entity.getUser_id());
                    pstmt.setInt(3, entity.getQuantity_disposed());
                    pstmt.setBigDecimal(4, entity.getTotal_cost());
                    pstmt.setBigDecimal(5, entity.getPharmacy_loss_amount());
                    pstmt.setBigDecimal(6, entity.getSupplier_compensation_amount());
                    
                    if (entity.getDisposal_date() != null) {
                        pstmt.setString(7, entity.getDisposal_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    pstmt.setString(8, entity.getReason());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لسجلات الإتلاف بسبب خرق القيود المحاسبية أو البيانات، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ السجلات المجمعة: " + e.getMessage());
            return false;
        }
    }

    private Disposal mapRowToDisposal(ResultSet rs) throws SQLException {
        Disposal disposal = new Disposal();
        disposal.setDisposal_id(rs.getInt("disposal_id"));
        disposal.setBatch_id(rs.getInt("batch_id"));
        disposal.setUser_id(rs.getInt("user_id"));
        disposal.setQuantity_disposed(rs.getInt("quantity_disposed"));
        disposal.setTotal_cost(rs.getBigDecimal("total_cost"));
        disposal.setPharmacy_loss_amount(rs.getBigDecimal("pharmacy_loss_amount"));
        disposal.setSupplier_compensation_amount(rs.getBigDecimal("supplier_compensation_amount"));
        
        String dateStr = rs.getString("disposal_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            disposal.setDisposal_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        disposal.setReason(rs.getString("reason"));
        
        return disposal;
    }

    @Override
    public List<Disposal> findByBatchId(int batchId) {
        List<Disposal> disposals = new ArrayList<>();
        String sql = "SELECT * FROM Disposals WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, batchId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    disposals.add(mapRowToDisposal(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجلات الإتلاف للتشغيلة: " + e.getMessage());
        }
        return disposals;
    }

    @Override
    public List<Disposal> findByUserId(int userId) {
        List<Disposal> disposals = new ArrayList<>();
        String sql = "SELECT * FROM Disposals WHERE user_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    disposals.add(mapRowToDisposal(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجلات الإتلاف للمستخدم: " + e.getMessage());
        }
        return disposals;
    }

    @Override
    public List<Disposal> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Disposal> disposals = new ArrayList<>();
        String sql = "SELECT * FROM Disposals WHERE disposal_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startDate.format(DATE_TIME_FORMATTER));
            pstmt.setString(2, endDate.format(DATE_TIME_FORMATTER));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    disposals.add(mapRowToDisposal(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجلات الإتلاف ضمن النطاق الزمني: " + e.getMessage());
        }
        return disposals;
    }
}