// مسار الملف: src/main/java/com/pharmacy/dao/impl/inventory/InventoryAdjustmentDAOImpl.java

package com.pharmacy.dao.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.InventoryAdjustmentDAO;
import com.pharmacy.models.inventory.InventoryAdjustment;
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

public class InventoryAdjustmentDAOImpl implements InventoryAdjustmentDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<InventoryAdjustment> create(InventoryAdjustment entity) {
        String sql = "INSERT INTO Inventory_Adjustments (batch_id, user_id, system_quantity, actual_quantity, difference, adjustment_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        // التدقيق الحسابي الإجباري: إعادة حساب الفرق برمجياً
        int calculatedDifference = entity.getActual_quantity() - entity.getSystem_quantity();
        
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getBatch_id());
            pstmt.setInt(2, entity.getUser_id());
            pstmt.setInt(3, entity.getSystem_quantity());
            pstmt.setInt(4, entity.getActual_quantity());
            pstmt.setInt(5, calculatedDifference);
            
            if (entity.getAdjustment_date() != null) {
                pstmt.setString(6, entity.getAdjustment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            if (entity.getNotes() != null) {
                pstmt.setString(7, entity.getNotes());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setAdjustment_id(generatedKeys.getInt(1));
                        // تحديث الكائن بالقيمة المحسوبة الفعيلة
                        entity.setDifference(calculatedDifference);
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج تسوية الجرد. يرجى التحقق من القيود (actual_quantity >= 0) أو المفاتيح الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }
    @Override
    public boolean executeAdjustmentTransaction(InventoryAdjustment adjustment, int newQuantity) {
        String updateBatchSql = "UPDATE Batches SET quantity = ? WHERE batch_id = ?";
        String insertAdjSql = "INSERT INTO Inventory_Adjustments (batch_id, user_id, system_quantity, actual_quantity, difference, adjustment_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnectionManager.getInstance().getConnection();
            // إيقاف الحفظ التلقائي لنتمكن من التحكم بالعمليتين معاً
            conn.setAutoCommit(false); 

            // 1. تحديث كمية الطبخة
            try (PreparedStatement psBatch = conn.prepareStatement(updateBatchSql)) {
                psBatch.setInt(1, newQuantity);
                psBatch.setInt(2, adjustment.getBatch_id());
                int batchUpdated = psBatch.executeUpdate();
                if (batchUpdated == 0) {
                    throw new SQLException("فشل تحديث الطبخة، لم يتم العثور عليها.");
                }
            }

            // 2. تسجيل حركة التسوية
            try (PreparedStatement psAdj = conn.prepareStatement(insertAdjSql)) {
                psAdj.setInt(1, adjustment.getBatch_id());
                psAdj.setInt(2, adjustment.getUser_id());
                psAdj.setInt(3, adjustment.getSystem_quantity());
                psAdj.setInt(4, adjustment.getActual_quantity());
                psAdj.setInt(5, adjustment.getDifference());
                
                if (adjustment.getAdjustment_date() != null) {
                    psAdj.setString(6, adjustment.getAdjustment_date().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } else {
                    psAdj.setString(6, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                
                psAdj.setString(7, adjustment.getNotes());
                psAdj.executeUpdate();
            }

            // 3. اعتماد العمليتين معاً (Commit)
            conn.commit();
            return true;

        } catch (SQLException e) {
            // 4. في حال فشل أي خطوة، التراجع عن كل شيء (Rollback)
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("[خطأ محاسبي - Transaction Failed]: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Optional<InventoryAdjustment> findById(Integer id) {
        String sql = "SELECT * FROM Inventory_Adjustments WHERE adjustment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAdjustment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن سجل تسوية الجرد: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<InventoryAdjustment> findAll() {
        List<InventoryAdjustment> adjustments = new ArrayList<>();
        String sql = "SELECT * FROM Inventory_Adjustments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                adjustments.add(mapRowToAdjustment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة سجلات تسوية الجرد: " + e.getMessage());
        }
        return adjustments;
    }
   
    
    @Override
    public List<InventoryAdjustment> findAll(int limit, int offset) {
        List<InventoryAdjustment> adjustments = new ArrayList<>();
        String sql = "SELECT * FROM Inventory_Adjustments LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    adjustments.add(mapRowToAdjustment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب سجلات تسوية الجرد (Pagination): " + e.getMessage());
        }
        return adjustments;
    }

    @Override
    public boolean update(InventoryAdjustment entity) {
        String sql = "UPDATE Inventory_Adjustments SET batch_id = ?, user_id = ?, system_quantity = ?, actual_quantity = ?, difference = ?, adjustment_date = ?, notes = ? WHERE adjustment_id = ?";
        
        // التدقيق الحسابي الإجباري
        int calculatedDifference = entity.getActual_quantity() - entity.getSystem_quantity();
        
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getBatch_id());
            pstmt.setInt(2, entity.getUser_id());
            pstmt.setInt(3, entity.getSystem_quantity());
            pstmt.setInt(4, entity.getActual_quantity());
            pstmt.setInt(5, calculatedDifference);
            
            if (entity.getAdjustment_date() != null) {
                pstmt.setString(6, entity.getAdjustment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }
            
            if (entity.getNotes() != null) {
                pstmt.setString(7, entity.getNotes());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            pstmt.setInt(8, entity.getAdjustment_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث سجل تسوية الجرد: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        // حظر أمني ورقابي صارم
        System.err.println("[إجراء محظور] يُمنع حذف سجلات تسوية الجرد لأسباب رقابية.");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Inventory_Adjustments WHERE adjustment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود سجل التسوية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Inventory_Adjustments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي سجلات الجرد: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<InventoryAdjustment> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Inventory_Adjustments (batch_id, user_id, system_quantity, actual_quantity, difference, adjustment_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (InventoryAdjustment entity : entities) {
                    
                    int calculatedDifference = entity.getActual_quantity() - entity.getSystem_quantity();
                    
                    pstmt.setInt(1, entity.getBatch_id());
                    pstmt.setInt(2, entity.getUser_id());
                    pstmt.setInt(3, entity.getSystem_quantity());
                    pstmt.setInt(4, entity.getActual_quantity());
                    pstmt.setInt(5, calculatedDifference);
                    
                    if (entity.getAdjustment_date() != null) {
                        pstmt.setString(6, entity.getAdjustment_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    if (entity.getNotes() != null) {
                        pstmt.setString(7, entity.getNotes());
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
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لتسويات الجرد، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private InventoryAdjustment mapRowToAdjustment(ResultSet rs) throws SQLException {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setAdjustment_id(rs.getInt("adjustment_id"));
        adjustment.setBatch_id(rs.getInt("batch_id"));
        adjustment.setUser_id(rs.getInt("user_id"));
        adjustment.setSystem_quantity(rs.getInt("system_quantity"));
        adjustment.setActual_quantity(rs.getInt("actual_quantity"));
        adjustment.setDifference(rs.getInt("difference"));
        
        String dateStr = rs.getString("adjustment_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            adjustment.setAdjustment_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        adjustment.setNotes(rs.getString("notes"));
        
        return adjustment;
    }

    @Override
    public List<InventoryAdjustment> findByBatchId(int batchId) {
        List<InventoryAdjustment> adjustments = new ArrayList<>();
        String sql = "SELECT * FROM Inventory_Adjustments WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, batchId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    adjustments.add(mapRowToAdjustment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن تسويات الجرد للتشغيلة: " + e.getMessage());
        }
        return adjustments;
    }

    @Override
    public List<InventoryAdjustment> findByUserId(int userId) {
        List<InventoryAdjustment> adjustments = new ArrayList<>();
        String sql = "SELECT * FROM Inventory_Adjustments WHERE user_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    adjustments.add(mapRowToAdjustment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن تسويات الجرد للمستخدم: " + e.getMessage());
        }
        return adjustments;
    }

    @Override
    public List<InventoryAdjustment> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<InventoryAdjustment> adjustments = new ArrayList<>();
        String sql = "SELECT * FROM Inventory_Adjustments WHERE adjustment_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startDate.format(DATE_TIME_FORMATTER));
            pstmt.setString(2, endDate.format(DATE_TIME_FORMATTER));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    adjustments.add(mapRowToAdjustment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن تسويات الجرد ضمن النطاق الزمني: " + e.getMessage());
        }
        return adjustments;
    }
    
    
}