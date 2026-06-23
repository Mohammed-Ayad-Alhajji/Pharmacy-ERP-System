// مسار الملف: src/main/java/com/pharmacy/dao/impl/inventory/BatchDAOImpl.java

package com.pharmacy.dao.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.BatchDAO;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BatchDAOImpl implements BatchDAO {

    @Override
    public Optional<Batch> create(Batch entity) {
        String sql = "INSERT INTO Batches (med_id, batch_number, mfg_date, exp_date, quantity, buy_box_cost, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getMed_id());
            pstmt.setString(2, entity.getBatch_number());
            pstmt.setString(3, entity.getMfg_date().toString()); // تحويل LocalDate إلى نص YYYY-MM-DD
            pstmt.setString(4, entity.getExp_date().toString());
            pstmt.setInt(5, entity.getQuantity());
            pstmt.setBigDecimal(6, entity.getBuy_box_cost());
            pstmt.setInt(7, entity.getIs_active());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setBatch_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج التشغيلة. يرجى التحقق من القيود (كمية أو سعر سالب، أو تعارض في تواريخ الصلاحية): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Batch> findById(Integer id) {
        String sql = "SELECT * FROM Batches WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات التشغيلة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Batch> findAll() {
        List<Batch> batches = new ArrayList<>();
        String sql = "SELECT * FROM Batches";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                batches.add(mapRowToBatch(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة التشغيلات: " + e.getMessage());
        }
        return batches;
    }

    @Override
    public List<Batch> findAll(int limit, int offset) {
        List<Batch> batches = new ArrayList<>();
        String sql = "SELECT * FROM Batches LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapRowToBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة التشغيلات (Pagination): " + e.getMessage());
        }
        return batches;
    }

    @Override
    public boolean update(Batch entity) {
        String sql = "UPDATE Batches SET med_id = ?, batch_number = ?, mfg_date = ?, exp_date = ?, quantity = ?, buy_box_cost = ?, is_active = ? WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getMed_id());
            pstmt.setString(2, entity.getBatch_number());
            pstmt.setString(3, entity.getMfg_date().toString());
            pstmt.setString(4, entity.getExp_date().toString());
            pstmt.setInt(5, entity.getQuantity());
            pstmt.setBigDecimal(6, entity.getBuy_box_cost());
            pstmt.setInt(7, entity.getIs_active());
            pstmt.setInt(8, entity.getBatch_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث التشغيلة. يرجى التحقق من التواريخ المنطقية أو الكميات/الأسعار السالبة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Batches WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حذف التشغيلة. تم حظر الحذف (ON DELETE RESTRICT) لارتباطها بسجلات مبيعات، مشتريات، مرتجعات، أو إتلاف: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Batches WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود التشغيلة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Batches";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي التشغيلات: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Batch> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Batches (med_id, batch_number, mfg_date, exp_date, quantity, buy_box_cost, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Batch entity : entities) {
                    pstmt.setInt(1, entity.getMed_id());
                    pstmt.setString(2, entity.getBatch_number());
                    pstmt.setString(3, entity.getMfg_date().toString());
                    pstmt.setString(4, entity.getExp_date().toString());
                    pstmt.setInt(5, entity.getQuantity());
                    pstmt.setBigDecimal(6, entity.getBuy_box_cost());
                    pstmt.setInt(7, entity.getIs_active());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للتشغيلات، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ التشغيلات المجمعة: " + e.getMessage());
            return false;
        }
    }

    private Batch mapRowToBatch(ResultSet rs) throws SQLException {
        Batch batch = new Batch();
        batch.setBatch_id(rs.getInt("batch_id"));
        batch.setMed_id(rs.getInt("med_id"));
        batch.setBatch_number(rs.getString("batch_number"));
        
        // تحويل النصوص القادمة من SQLite إلى كائنات LocalDate
        batch.setMfg_date(LocalDate.parse(rs.getString("mfg_date")));
        batch.setExp_date(LocalDate.parse(rs.getString("exp_date")));
        
        batch.setQuantity(rs.getInt("quantity"));
        batch.setBuy_box_cost(rs.getBigDecimal("buy_box_cost"));
        batch.setIs_active(rs.getInt("is_active"));
        
        return batch;
    }

    @Override
    public List<Batch> findByMedicineId(int medId) {
        List<Batch> batches = new ArrayList<>();
        String sql = "SELECT * FROM Batches WHERE med_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, medId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapRowToBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب التشغيلات المرتبطة بالدواء: " + e.getMessage());
        }
        return batches;
    }

    @Override
    public List<Batch> findActiveBatchesByMedicine(int medId) {
        List<Batch> batches = new ArrayList<>();
        String sql = "SELECT * FROM Batches WHERE med_id = ? AND is_active = 1";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, medId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapRowToBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب التشغيلات الفعالة المرتبطة بالدواء: " + e.getMessage());
        }
        return batches;
    }

    @Override
    public List<Batch> findNearExpiryBatches(int daysThreshold) {
        List<Batch> batches = new ArrayList<>();
        LocalDate targetDate = LocalDate.now().plusDays(daysThreshold);
        String sql = "SELECT * FROM Batches WHERE quantity > 0 AND is_active = 1 AND exp_date <= ? AND exp_date >= date('now') ORDER BY exp_date ASC";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, targetDate.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapRowToBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب تقرير التشغيلات المقاربة على الانتهاء: " + e.getMessage());
        }
        return batches;
    }

    @Override
    public Optional<Batch> findByBatchNumberAndMedicineId(String batchNumber, int medId) {
        String sql = "SELECT * FROM Batches WHERE batch_number = ? AND med_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, batchNumber);
            pstmt.setInt(2, medId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل البحث عن التشغيلة برقمها ومعرف الدواء: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Batch> findValidBatchesOrderedByExpiry(int medId) {
        List<Batch> batches = new ArrayList<>();
        String sql = "SELECT * FROM Batches WHERE med_id = ? AND quantity > 0 AND is_active = 1 ORDER BY exp_date ASC";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, medId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapRowToBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب التشغيلات الصالحة للبيع (FEFO): " + e.getMessage());
        }
        return batches;
    }
    
    @Override
    public Optional<Batch> getLastBatchByMedicineId(int medId) {
        // نجبر قاعدة البيانات على جلب أعلى رقم معرّف ID (وهو الأحدث إدخالاً)
        String sql = "SELECT * FROM Batches WHERE med_id = ? ORDER BY batch_id DESC LIMIT 1";
        
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, medId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBatch(rs)); 
                }
            }
        } catch (SQLException e) {
            System.err.println("فشل جلب أحدث طبخة: " + e.getMessage());
        }
        return Optional.empty();
    }
}