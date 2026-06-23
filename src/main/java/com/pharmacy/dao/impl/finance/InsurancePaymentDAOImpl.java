// مسار الملف: src/main/java/com/pharmacy/dao/impl/finance/InsurancePaymentDAOImpl.java

package com.pharmacy.dao.impl.finance;

import com.pharmacy.dao.interfaces.finance.InsurancePaymentDAO;
import com.pharmacy.models.finance.InsurancePayment;
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

public class InsurancePaymentDAOImpl implements InsurancePaymentDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<InsurancePayment> create(InsurancePayment entity) {
        String sql = "INSERT INTO Insurance_Payments (insurance_id, sale_id, shift_id, amount_paid, payment_method, reference_number, payment_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getInsurance_id());
            
            if (entity.getSale_id() == null || entity.getSale_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getSale_id());
            }
            
            pstmt.setInt(3, entity.getShift_id());
            pstmt.setBigDecimal(4, entity.getAmount_paid());
            
            if (entity.getPayment_method() != null) {
                pstmt.setString(5, entity.getPayment_method());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }

            if (entity.getReference_number() != null) {
                pstmt.setString(6, entity.getReference_number());
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }

            if (entity.getPayment_date() != null) {
                pstmt.setString(7, entity.getPayment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
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
            System.err.println("[خطأ تنفيذي] فشل إدراج دفعة التأمين. تحقق من قيود المبالغ الموجبة (amount_paid > 0) وصحة المعرفات الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<InsurancePayment> findById(Integer id) {
        String sql = "SELECT * FROM Insurance_Payments WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الدفعة المالية للتأمين: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<InsurancePayment> findAll() {
        List<InsurancePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Payments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                payments.add(mapRowToInsurancePayment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة الدفعات المالية للتأمين: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<InsurancePayment> findAll(int limit, int offset) {
        List<InsurancePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Payments LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب الدفعات المالية المجدولة للتأمين: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public boolean update(InsurancePayment entity) {
        String sql = "UPDATE Insurance_Payments SET insurance_id = ?, sale_id = ?, shift_id = ?, amount_paid = ?, payment_method = ?, reference_number = ?, payment_date = ? WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getInsurance_id());
            
            if (entity.getSale_id() == null || entity.getSale_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getSale_id());
            }
            
            pstmt.setInt(3, entity.getShift_id());
            pstmt.setBigDecimal(4, entity.getAmount_paid());
            
            if (entity.getPayment_method() != null) {
                pstmt.setString(5, entity.getPayment_method());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }

            if (entity.getReference_number() != null) {
                pstmt.setString(6, entity.getReference_number());
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }

            if (entity.getPayment_date() != null) {
                pstmt.setString(7, entity.getPayment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            pstmt.setInt(8, entity.getPayment_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات الدفعة المالية للتأمين: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف الدفعات الخاصة بالتأمين لحماية سجلات التسوية البنكية والصندوق");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Insurance_Payments WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود دفعة التأمين: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Insurance_Payments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي دفعات التأمين: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<InsurancePayment> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Insurance_Payments (insurance_id, sale_id, shift_id, amount_paid, payment_method, reference_number, payment_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (InsurancePayment entity : entities) {
                    pstmt.setInt(1, entity.getInsurance_id());
                    
                    if (entity.getSale_id() == null || entity.getSale_id() == 0) {
                        pstmt.setNull(2, Types.INTEGER);
                    } else {
                        pstmt.setInt(2, entity.getSale_id());
                    }
                    
                    pstmt.setInt(3, entity.getShift_id());
                    pstmt.setBigDecimal(4, entity.getAmount_paid());
                    
                    if (entity.getPayment_method() != null) {
                        pstmt.setString(5, entity.getPayment_method());
                    } else {
                        pstmt.setNull(5, Types.VARCHAR);
                    }

                    if (entity.getReference_number() != null) {
                        pstmt.setString(6, entity.getReference_number());
                    } else {
                        pstmt.setNull(6, Types.VARCHAR);
                    }
                    
                    if (entity.getPayment_date() != null) {
                        pstmt.setString(7, entity.getPayment_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لدفعات التأمين. تم التراجع (Rollback). تأكد من القيود المحاسبية: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private InsurancePayment mapRowToInsurancePayment(ResultSet rs) throws SQLException {
        InsurancePayment payment = new InsurancePayment();
        payment.setPayment_id(rs.getInt("payment_id"));
        payment.setInsurance_id(rs.getInt("insurance_id"));
        
        int saleId = rs.getInt("sale_id");
        payment.setSale_id(rs.wasNull() ? null : saleId);
        
        payment.setShift_id(rs.getInt("shift_id"));
        payment.setAmount_paid(rs.getBigDecimal("amount_paid"));
        payment.setPayment_method(rs.getString("payment_method"));
        payment.setReference_number(rs.getString("reference_number"));
        
        String dateStr = rs.getString("payment_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            payment.setPayment_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        return payment;
    }

    @Override
    public List<InsurancePayment> findByInsuranceId(int insuranceId) {
        List<InsurancePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Payments WHERE insurance_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, insuranceId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن دفعات شركة التأمين: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<InsurancePayment> findBySaleId(int saleId) {
        List<InsurancePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Payments WHERE sale_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, saleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات المرتبطة بالفاتورة: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<InsurancePayment> findByShiftId(int shiftId) {
        List<InsurancePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Payments WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shiftId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن دفعات الوردية: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<InsurancePayment> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<InsurancePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Payments WHERE payment_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endDateTime = endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات ضمن النطاق الزمني: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<InsurancePayment> findByPaymentMethod(String method) {
        List<InsurancePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Payments WHERE payment_method = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, method);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات حسب طريقة الدفع: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public Optional<InsurancePayment> findByReferenceNumber(String referenceNumber) {
        String sql = "SELECT * FROM Insurance_Payments WHERE reference_number = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, referenceNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToInsurancePayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعة برقم المرجع/الحوالة: " + e.getMessage());
        }
        return Optional.empty();
    }
}