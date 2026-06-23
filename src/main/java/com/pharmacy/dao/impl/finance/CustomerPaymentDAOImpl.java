// مسار الملف: src/main/java/com/pharmacy/dao/impl/finance/CustomerPaymentDAOImpl.java

package com.pharmacy.dao.impl.finance;

import com.pharmacy.dao.interfaces.finance.CustomerPaymentDAO;
import com.pharmacy.models.finance.CustomerPayment;
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

public class CustomerPaymentDAOImpl implements CustomerPaymentDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<CustomerPayment> create(CustomerPayment entity) {
        String sql = "INSERT INTO Customer_Payments (customer_id, sale_id, shift_id, amount_paid, payment_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getCustomer_id());
            
            if (entity.getSale_id() == null || entity.getSale_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getSale_id());
            }
            
            pstmt.setInt(3, entity.getShift_id());
            pstmt.setBigDecimal(4, entity.getAmount_paid());
            
            if (entity.getPayment_date() != null) {
                pstmt.setString(5, entity.getPayment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(5, LocalDateTime.now().format(DATE_TIME_FORMATTER));
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
            System.err.println("[خطأ تنفيذي] فشل إدراج الدفعة. تحقق من قيود المبالغ الموجبة (amount_paid > 0) وصحة المعرفات الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<CustomerPayment> findById(Integer id) {
        String sql = "SELECT * FROM Customer_Payments WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCustomerPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الدفعة المالية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<CustomerPayment> findAll() {
        List<CustomerPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Customer_Payments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                payments.add(mapRowToCustomerPayment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة الدفعات المالية: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<CustomerPayment> findAll(int limit, int offset) {
        List<CustomerPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Customer_Payments LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToCustomerPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب الدفعات المالية المجدولة: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public boolean update(CustomerPayment entity) {
        String sql = "UPDATE Customer_Payments SET customer_id = ?, sale_id = ?, shift_id = ?, amount_paid = ?, payment_date = ? WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getCustomer_id());
            
            if (entity.getSale_id() == null || entity.getSale_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getSale_id());
            }
            
            pstmt.setInt(3, entity.getShift_id());
            pstmt.setBigDecimal(4, entity.getAmount_paid());
            
            if (entity.getPayment_date() != null) {
                pstmt.setString(5, entity.getPayment_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            pstmt.setInt(6, entity.getPayment_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات الدفعة المالية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف الدفعات المالية للزبائن لحماية عهدة الصندوق");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Customer_Payments WHERE payment_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الدفعة المالية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Customer_Payments";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الدفعات المالية: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<CustomerPayment> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Customer_Payments (customer_id, sale_id, shift_id, amount_paid, payment_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (CustomerPayment entity : entities) {
                    pstmt.setInt(1, entity.getCustomer_id());
                    
                    if (entity.getSale_id() == null || entity.getSale_id() == 0) {
                        pstmt.setNull(2, Types.INTEGER);
                    } else {
                        pstmt.setInt(2, entity.getSale_id());
                    }
                    
                    pstmt.setInt(3, entity.getShift_id());
                    pstmt.setBigDecimal(4, entity.getAmount_paid());
                    
                    if (entity.getPayment_date() != null) {
                        pstmt.setString(5, entity.getPayment_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(5, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للدفعات المالية. تم التراجع (Rollback). تأكد من القيود المحاسبية: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private CustomerPayment mapRowToCustomerPayment(ResultSet rs) throws SQLException {
        CustomerPayment payment = new CustomerPayment();
        payment.setPayment_id(rs.getInt("payment_id"));
        payment.setCustomer_id(rs.getInt("customer_id"));
        
        int saleId = rs.getInt("sale_id");
        payment.setSale_id(rs.wasNull() ? null : saleId);
        
        payment.setShift_id(rs.getInt("shift_id"));
        payment.setAmount_paid(rs.getBigDecimal("amount_paid"));
        
        String dateStr = rs.getString("payment_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            payment.setPayment_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        return payment;
    }

    @Override
    public List<CustomerPayment> findByCustomerId(int customerId) {
        List<CustomerPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Customer_Payments WHERE customer_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToCustomerPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن دفعات الزبون: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<CustomerPayment> findBySaleId(int saleId) {
        List<CustomerPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Customer_Payments WHERE sale_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, saleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToCustomerPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات المرتبطة بالفاتورة: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<CustomerPayment> findByShiftId(int shiftId) {
        List<CustomerPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Customer_Payments WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shiftId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToCustomerPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن دفعات الوردية: " + e.getMessage());
        }
        return payments;
    }

    @Override
    public List<CustomerPayment> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<CustomerPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM Customer_Payments WHERE payment_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endDateTime = endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRowToCustomerPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدفعات ضمن النطاق الزمني: " + e.getMessage());
        }
        return payments;
    }
}