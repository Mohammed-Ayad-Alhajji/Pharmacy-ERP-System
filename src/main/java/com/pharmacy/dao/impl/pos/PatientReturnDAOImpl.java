// مسار الملف: src/main/java/com/pharmacy/dao/impl/pos/PatientReturnDAOImpl.java

package com.pharmacy.dao.impl.pos;

import com.pharmacy.dao.interfaces.pos.PatientReturnDAO;
import com.pharmacy.models.pos.PatientReturn;
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

public class PatientReturnDAOImpl implements PatientReturnDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<PatientReturn> create(PatientReturn entity) {
        String sql = "INSERT INTO Patient_Returns (detail_id, shift_id, quantity_returned, patient_cash_refund, insurance_canceled_amount, return_date, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getDetail_id());
            pstmt.setInt(2, entity.getShift_id());
            pstmt.setInt(3, entity.getQuantity_returned());
            pstmt.setBigDecimal(4, entity.getPatient_cash_refund());
            pstmt.setBigDecimal(5, entity.getInsurance_canceled_amount());
            
            if (entity.getReturn_date() != null) {
                pstmt.setString(6, entity.getReturn_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            if (entity.getReason() != null) {
                pstmt.setString(7, entity.getReason());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setReturn_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج المرتجع. تأكد من قيود CHECK للكمية والمبالغ المالية أو المعرفات الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<PatientReturn> findById(Integer id) {
        String sql = "SELECT * FROM Patient_Returns WHERE return_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPatientReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات المرتجع: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<PatientReturn> findAll() {
        List<PatientReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Patient_Returns";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                returns.add(mapRowToPatientReturn(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة المرتجعات: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<PatientReturn> findAll(int limit, int offset) {
        List<PatientReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Patient_Returns LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToPatientReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة المرتجعات المجدولة (Pagination): " + e.getMessage());
        }
        return returns;
    }

    @Override
    public boolean update(PatientReturn entity) {
        String sql = "UPDATE Patient_Returns SET detail_id = ?, shift_id = ?, quantity_returned = ?, patient_cash_refund = ?, insurance_canceled_amount = ?, return_date = ?, reason = ? WHERE return_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getDetail_id());
            pstmt.setInt(2, entity.getShift_id());
            pstmt.setInt(3, entity.getQuantity_returned());
            pstmt.setBigDecimal(4, entity.getPatient_cash_refund());
            pstmt.setBigDecimal(5, entity.getInsurance_canceled_amount());
            
            if (entity.getReturn_date() != null) {
                pstmt.setString(6, entity.getReturn_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }
            
            if (entity.getReason() != null) {
                pstmt.setString(7, entity.getReason());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            pstmt.setInt(8, entity.getReturn_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات المرتجع. تأكد من القيود المحاسبية والمعرفات الأجنبية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف المرتجعات لحماية عهدة الصندوق");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Patient_Returns WHERE return_id = ?";
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
        String sql = "SELECT COUNT(*) FROM Patient_Returns";
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
    public boolean saveAll(List<PatientReturn> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Patient_Returns (detail_id, shift_id, quantity_returned, patient_cash_refund, insurance_canceled_amount, return_date, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (PatientReturn entity : entities) {
                    pstmt.setInt(1, entity.getDetail_id());
                    pstmt.setInt(2, entity.getShift_id());
                    pstmt.setInt(3, entity.getQuantity_returned());
                    pstmt.setBigDecimal(4, entity.getPatient_cash_refund());
                    pstmt.setBigDecimal(5, entity.getInsurance_canceled_amount());
                    
                    if (entity.getReturn_date() != null) {
                        pstmt.setString(6, entity.getReturn_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    if (entity.getReason() != null) {
                        pstmt.setString(7, entity.getReason());
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
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للمرتجعات. تم التراجع (Rollback). تأكد من القيود المحاسبية: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ المرتجعات المجمعة: " + e.getMessage());
            return false;
        }
    }

    private PatientReturn mapRowToPatientReturn(ResultSet rs) throws SQLException {
        PatientReturn returnEntity = new PatientReturn();
        returnEntity.setReturn_id(rs.getInt("return_id"));
        returnEntity.setDetail_id(rs.getInt("detail_id"));
        returnEntity.setShift_id(rs.getInt("shift_id"));
        returnEntity.setQuantity_returned(rs.getInt("quantity_returned"));
        returnEntity.setPatient_cash_refund(rs.getBigDecimal("patient_cash_refund"));
        returnEntity.setInsurance_canceled_amount(rs.getBigDecimal("insurance_canceled_amount"));
        
        String dateStr = rs.getString("return_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            returnEntity.setReturn_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        returnEntity.setReason(rs.getString("reason"));
        
        return returnEntity;
    }

    @Override
    public List<PatientReturn> findBySaleDetailId(int detailId) {
        List<PatientReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Patient_Returns WHERE detail_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, detailId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToPatientReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مرتجعات تفصيل الفاتورة: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<PatientReturn> findByShiftId(int shiftId) {
        List<PatientReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Patient_Returns WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shiftId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToPatientReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مرتجعات الوردية: " + e.getMessage());
        }
        return returns;
    }

    @Override
    public List<PatientReturn> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<PatientReturn> returns = new ArrayList<>();
        String sql = "SELECT * FROM Patient_Returns WHERE return_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt =prepareStatement(conn, sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endDateTime = endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapRowToPatientReturn(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المرتجعات ضمن النطاق الزمني: " + e.getMessage());
        }
        return returns;
    }

    private PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }
}