// مسار الملف: src/main/java/com/pharmacy/dao/impl/purchasing/PurchaseDetailDAOImpl.java

package com.pharmacy.dao.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.PurchaseDetailDAO;
import com.pharmacy.models.purchasing.PurchaseDetail;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseDetailDAOImpl implements PurchaseDetailDAO {

    @Override
    public Optional<PurchaseDetail> create(PurchaseDetail entity) {
        String sql = "INSERT INTO Purchase_Details (purchase_id, batch_id, quantity_received, bonus_quantity, box_cost) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getPurchase_id());
            pstmt.setInt(2, entity.getBatch_id());
            pstmt.setInt(3, entity.getQuantity_received());
            pstmt.setInt(4, entity.getBonus_quantity());
            pstmt.setBigDecimal(5, entity.getBox_cost());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setPd_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج تفاصيل الفاتورة. تحقق من القيود (كمية <= 0، أو تكلفة سالبة) أو صحة المعرفات الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<PurchaseDetail> findById(Integer id) {
        String sql = "SELECT * FROM Purchase_Details WHERE pd_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPurchaseDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات تفصيل الفاتورة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<PurchaseDetail> findAll() {
        List<PurchaseDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM Purchase_Details";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                details.add(mapRowToPurchaseDetail(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة تفاصيل الفواتير: " + e.getMessage());
        }
        return details;
    }

    @Override
    public List<PurchaseDetail> findAll(int limit, int offset) {
        List<PurchaseDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM Purchase_Details LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(mapRowToPurchaseDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة تفاصيل الفواتير (Pagination): " + e.getMessage());
        }
        return details;
    }

    @Override
    public boolean update(PurchaseDetail entity) {
        String sql = "UPDATE Purchase_Details SET purchase_id = ?, batch_id = ?, quantity_received = ?, bonus_quantity = ?, box_cost = ? WHERE pd_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getPurchase_id());
            pstmt.setInt(2, entity.getBatch_id());
            pstmt.setInt(3, entity.getQuantity_received());
            pstmt.setInt(4, entity.getBonus_quantity());
            pstmt.setBigDecimal(5, entity.getBox_cost());
            pstmt.setInt(6, entity.getPd_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث تفاصيل الفاتورة. تحقق من القيود (CHECK/FOREIGN KEY): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Purchase_Details WHERE pd_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حذف تفصيل الفاتورة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Purchase_Details WHERE pd_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود تفصيل الفاتورة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Purchase_Details";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي تفاصيل الفواتير: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<PurchaseDetail> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Purchase_Details (purchase_id, batch_id, quantity_received, bonus_quantity, box_cost) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (PurchaseDetail entity : entities) {
                    pstmt.setInt(1, entity.getPurchase_id());
                    pstmt.setInt(2, entity.getBatch_id());
                    pstmt.setInt(3, entity.getQuantity_received());
                    pstmt.setInt(4, entity.getBonus_quantity());
                    pstmt.setBigDecimal(5, entity.getBox_cost());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لتفاصيل الفاتورة، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ التفاصيل المجمعة: " + e.getMessage());
            return false;
        }
    }

    private PurchaseDetail mapRowToPurchaseDetail(ResultSet rs) throws SQLException {
        PurchaseDetail detail = new PurchaseDetail();
        detail.setPd_id(rs.getInt("pd_id"));
        detail.setPurchase_id(rs.getInt("purchase_id"));
        detail.setBatch_id(rs.getInt("batch_id"));
        detail.setQuantity_received(rs.getInt("quantity_received"));
        detail.setBonus_quantity(rs.getInt("bonus_quantity"));
        detail.setBox_cost(rs.getBigDecimal("box_cost"));
        return detail;
    }

    @Override
    public List<PurchaseDetail> findByPurchaseId(int purchaseId) {
        List<PurchaseDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM Purchase_Details WHERE purchase_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, purchaseId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(mapRowToPurchaseDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن تفاصيل الفاتورة: " + e.getMessage());
        }
        return details;
    }

    @Override
    public Optional<PurchaseDetail> findByBatchId(int batchId) {
        String sql = "SELECT * FROM Purchase_Details WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, batchId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPurchaseDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن تفصيل الفاتورة المرتبط بالتشغيلة: " + e.getMessage());
        }
        return Optional.empty();
    }
}