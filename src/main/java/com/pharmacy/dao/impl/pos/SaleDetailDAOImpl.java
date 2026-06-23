// مسار الملف: src/main/java/com/pharmacy/dao/impl/pos/SaleDetailDAOImpl.java

package com.pharmacy.dao.impl.pos;

import com.pharmacy.dao.interfaces.pos.SaleDetailDAO;
import com.pharmacy.models.pos.SaleDetail;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleDetailDAOImpl implements SaleDetailDAO {

    @Override
    public Optional<SaleDetail> create(SaleDetail entity) {
        String sql = "INSERT INTO Sale_Details (sale_id, batch_id, quantity_sold, unit_sell_price, patient_share, insurance_share, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getSale_id());
            pstmt.setInt(2, entity.getBatch_id());
            pstmt.setInt(3, entity.getQuantity_sold());
            pstmt.setBigDecimal(4, entity.getUnit_sell_price());
            pstmt.setBigDecimal(5, entity.getPatient_share());
            pstmt.setBigDecimal(6, entity.getInsurance_share());
            pstmt.setBigDecimal(7, entity.getSubtotal());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setDetail_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج تفصيل الفاتورة. تحقق من القيود: القيمة سالبة، أو خرق المعادلة المحاسبية (subtotal = patient_share + insurance_share): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<SaleDetail> findById(Integer id) {
        String sql = "SELECT * FROM Sale_Details WHERE detail_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSaleDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات تفصيل الفاتورة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<SaleDetail> findAll() {
        List<SaleDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Details";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                details.add(mapRowToSaleDetail(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة تفاصيل المبيعات: " + e.getMessage());
        }
        return details;
    }

    @Override
    public List<SaleDetail> findAll(int limit, int offset) {
        List<SaleDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Details LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(mapRowToSaleDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة تفاصيل المبيعات المجدولة: " + e.getMessage());
        }
        return details;
    }

    @Override
    public boolean update(SaleDetail entity) {
        String sql = "UPDATE Sale_Details SET sale_id = ?, batch_id = ?, quantity_sold = ?, unit_sell_price = ?, patient_share = ?, insurance_share = ?, subtotal = ? WHERE detail_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getSale_id());
            pstmt.setInt(2, entity.getBatch_id());
            pstmt.setInt(3, entity.getQuantity_sold());
            pstmt.setBigDecimal(4, entity.getUnit_sell_price());
            pstmt.setBigDecimal(5, entity.getPatient_share());
            pstmt.setBigDecimal(6, entity.getInsurance_share());
            pstmt.setBigDecimal(7, entity.getSubtotal());
            pstmt.setInt(8, entity.getDetail_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث تفصيل الفاتورة. تحقق من القيود الرياضية والمحاسبية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف تفاصيل فواتير المبيعات فيزيائياً لارتباطها المباشر بحركة المخزون والذمم المالية. يجب استخدام آلية المرتجعات عوضاً عن ذلك.");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Sale_Details WHERE detail_id = ?";
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
        String sql = "SELECT COUNT(*) FROM Sale_Details";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي تفاصيل المبيعات: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<SaleDetail> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Sale_Details (sale_id, batch_id, quantity_sold, unit_sell_price, patient_share, insurance_share, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (SaleDetail entity : entities) {
                    pstmt.setInt(1, entity.getSale_id());
                    pstmt.setInt(2, entity.getBatch_id());
                    pstmt.setInt(3, entity.getQuantity_sold());
                    pstmt.setBigDecimal(4, entity.getUnit_sell_price());
                    pstmt.setBigDecimal(5, entity.getPatient_share());
                    pstmt.setBigDecimal(6, entity.getInsurance_share());
                    pstmt.setBigDecimal(7, entity.getSubtotal());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لتفاصيل الفاتورة. تم التراجع (Rollback). خرق للقيود المحاسبية أو الرياضية: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private SaleDetail mapRowToSaleDetail(ResultSet rs) throws SQLException {
        SaleDetail detail = new SaleDetail();
        detail.setDetail_id(rs.getInt("detail_id"));
        detail.setSale_id(rs.getInt("sale_id"));
        detail.setBatch_id(rs.getInt("batch_id"));
        detail.setQuantity_sold(rs.getInt("quantity_sold"));
        detail.setUnit_sell_price(rs.getBigDecimal("unit_sell_price"));
        detail.setPatient_share(rs.getBigDecimal("patient_share"));
        detail.setInsurance_share(rs.getBigDecimal("insurance_share"));
        detail.setSubtotal(rs.getBigDecimal("subtotal"));
        return detail;
    }

    @Override
    public List<SaleDetail> findBySaleId(int saleId) {
        List<SaleDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Details WHERE sale_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, saleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(mapRowToSaleDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن تفاصيل المبيعات المرتبطة بالفاتورة: " + e.getMessage());
        }
        return details;
    }

    @Override
    public List<SaleDetail> findByBatchId(int batchId) {
        List<SaleDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Details WHERE batch_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, batchId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(mapRowToSaleDetail(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن تفاصيل المبيعات المرتبطة بالتشغيلة (مسار التتبع الطبي): " + e.getMessage());
        }
        return details;
    }
}