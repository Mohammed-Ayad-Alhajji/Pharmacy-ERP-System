// مسار الملف: src/main/java/com/pharmacy/dao/impl/pos/SaleDAOImpl.java

package com.pharmacy.dao.impl.pos;

import com.pharmacy.dao.interfaces.pos.SaleDAO;
import com.pharmacy.models.pos.Sale;
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

public class SaleDAOImpl implements SaleDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<Sale> create(Sale entity) {
        // تحديث الاستعلام ليشمل الحقول الجديدة (doctor_name, patient_name)
        String sql = "INSERT INTO Sales (shift_id, customer_id, insurance_id, insurance_approval_code, prescription_image_path, doctor_name, patient_name, sale_date, total_amount, discount_amount, rounding_adjustment, total_patient_paid, total_customer_debt, total_insurance_debt, payment_method, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getShift_id());
            
            if (entity.getCustomer_id() == null || entity.getCustomer_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getCustomer_id());
            }
            
            if (entity.getInsurance_id() == null || entity.getInsurance_id() == 0) {
                pstmt.setNull(3, Types.INTEGER);
            } else {
                pstmt.setInt(3, entity.getInsurance_id());
            }
            
            if (entity.getInsurance_approval_code() != null) {
                pstmt.setString(4, entity.getInsurance_approval_code());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            
            if (entity.getPrescription_image_path() != null) {
                pstmt.setString(5, entity.getPrescription_image_path());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            // --- الحقول الجديدة للوصفة الطبية ---
            if (entity.getDoctor_name() != null) {
                pstmt.setString(6, entity.getDoctor_name());
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }

            if (entity.getPatient_name() != null) {
                pstmt.setString(7, entity.getPatient_name());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            // ------------------------------------
            
            if (entity.getSale_date() != null) {
                pstmt.setString(8, entity.getSale_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(8, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            // تمت إزاحة المؤشرات هنا بمقدار 2 لتتوافق مع الحقول الجديدة
            pstmt.setBigDecimal(9, entity.getTotal_amount());
            pstmt.setBigDecimal(10, entity.getDiscount_amount());
            pstmt.setBigDecimal(11, entity.getRounding_adjustment());
            pstmt.setBigDecimal(12, entity.getTotal_patient_paid());
            pstmt.setBigDecimal(13, entity.getTotal_customer_debt());
            pstmt.setBigDecimal(14, entity.getTotal_insurance_debt());
            pstmt.setString(15, entity.getPayment_method());
            
            if (entity.getStatus() != null) {
                pstmt.setString(16, entity.getStatus());
            } else {
                pstmt.setString(16, "Completed");
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setSale_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج الفاتورة. تأكد من قيود CHECK للمبالغ المالية وصحة المعرفات الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Sale> findById(Integer id) {
        String sql = "SELECT * FROM Sales WHERE sale_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الفاتورة: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Sale> findAll() {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                sales.add(mapRowToSale(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الفواتير: " + e.getMessage());
        }
        return sales;
    }

    @Override
    public List<Sale> findAll(int limit, int offset) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الفواتير (Pagination): " + e.getMessage());
        }
        return sales;
    }

    @Override
    public boolean update(Sale entity) {
        String sql = "UPDATE Sales SET shift_id = ?, customer_id = ?, insurance_id = ?, insurance_approval_code = ?, prescription_image_path = ?, sale_date = ?, total_amount = ?, discount_amount = ?, rounding_adjustment = ?, total_patient_paid = ?, total_customer_debt = ?, total_insurance_debt = ?, payment_method = ?, status = ? WHERE sale_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getShift_id());
            
            if (entity.getCustomer_id() == null || entity.getCustomer_id() == 0) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setInt(2, entity.getCustomer_id());
            }
            
            if (entity.getInsurance_id() == null || entity.getInsurance_id() == 0) {
                pstmt.setNull(3, Types.INTEGER);
            } else {
                pstmt.setInt(3, entity.getInsurance_id());
            }
            
            if (entity.getInsurance_approval_code() != null) {
                pstmt.setString(4, entity.getInsurance_approval_code());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            
            if (entity.getPrescription_image_path() != null) {
                pstmt.setString(5, entity.getPrescription_image_path());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            if (entity.getSale_date() != null) {
                pstmt.setString(6, entity.getSale_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }
            
            pstmt.setBigDecimal(7, entity.getTotal_amount());
            pstmt.setBigDecimal(8, entity.getDiscount_amount());
            pstmt.setBigDecimal(9, entity.getRounding_adjustment());
            pstmt.setBigDecimal(10, entity.getTotal_patient_paid());
            pstmt.setBigDecimal(11, entity.getTotal_customer_debt());
            pstmt.setBigDecimal(12, entity.getTotal_insurance_debt());
            pstmt.setString(13, entity.getPayment_method());
            pstmt.setString(14, entity.getStatus());
            pstmt.setInt(15, entity.getSale_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث الفاتورة. تأكد من صحة القيود المحاسبية والمعرفات الأجنبية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف فواتير المبيعات لأسباب محاسبية ورقابية");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Sales WHERE sale_id = ?";
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
        String sql = "SELECT COUNT(*) FROM Sales";
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
    public boolean saveAll(List<Sale> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Sales (shift_id, customer_id, insurance_id, insurance_approval_code, prescription_image_path, sale_date, total_amount, discount_amount, rounding_adjustment, total_patient_paid, total_customer_debt, total_insurance_debt, payment_method, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Sale entity : entities) {
                    pstmt.setInt(1, entity.getShift_id());
                    
                    if (entity.getCustomer_id() == null || entity.getCustomer_id() == 0) {
                        pstmt.setNull(2, Types.INTEGER);
                    } else {
                        pstmt.setInt(2, entity.getCustomer_id());
                    }
                    
                    if (entity.getInsurance_id() == null || entity.getInsurance_id() == 0) {
                        pstmt.setNull(3, Types.INTEGER);
                    } else {
                        pstmt.setInt(3, entity.getInsurance_id());
                    }
                    
                    if (entity.getInsurance_approval_code() != null) {
                        pstmt.setString(4, entity.getInsurance_approval_code());
                    } else {
                        pstmt.setNull(4, Types.VARCHAR);
                    }
                    
                    if (entity.getPrescription_image_path() != null) {
                        pstmt.setString(5, entity.getPrescription_image_path());
                    } else {
                        pstmt.setNull(5, Types.VARCHAR);
                    }
                    
                    if (entity.getSale_date() != null) {
                        pstmt.setString(6, entity.getSale_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(6, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    pstmt.setBigDecimal(7, entity.getTotal_amount());
                    pstmt.setBigDecimal(8, entity.getDiscount_amount());
                    pstmt.setBigDecimal(9, entity.getRounding_adjustment());
                    pstmt.setBigDecimal(10, entity.getTotal_patient_paid());
                    pstmt.setBigDecimal(11, entity.getTotal_customer_debt());
                    pstmt.setBigDecimal(12, entity.getTotal_insurance_debt());
                    pstmt.setString(13, entity.getPayment_method());
                    
                    if (entity.getStatus() != null) {
                        pstmt.setString(14, entity.getStatus());
                    } else {
                        pstmt.setString(14, "Completed");
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للفواتير. تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ الفواتير المجمعة: " + e.getMessage());
            return false;
        }
    }

    private Sale mapRowToSale(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setSale_id(rs.getInt("sale_id"));
        sale.setShift_id(rs.getInt("shift_id"));
        
        int customerId = rs.getInt("customer_id");
        sale.setCustomer_id(rs.wasNull() ? null : customerId);
        
        int insuranceId = rs.getInt("insurance_id");
        sale.setInsurance_id(rs.wasNull() ? null : insuranceId);
        
        sale.setInsurance_approval_code(rs.getString("insurance_approval_code"));
        sale.setPrescription_image_path(rs.getString("prescription_image_path"));
        
        // --- الحقول الجديدة للوصفة الطبية ---
        sale.setDoctor_name(rs.getString("doctor_name"));
        sale.setPatient_name(rs.getString("patient_name"));
        // ------------------------------------
        
        String dateStr = rs.getString("sale_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            sale.setSale_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        sale.setTotal_amount(rs.getBigDecimal("total_amount"));
        sale.setDiscount_amount(rs.getBigDecimal("discount_amount"));
        sale.setRounding_adjustment(rs.getBigDecimal("rounding_adjustment"));
        sale.setTotal_patient_paid(rs.getBigDecimal("total_patient_paid"));
        sale.setTotal_customer_debt(rs.getBigDecimal("total_customer_debt"));
        sale.setTotal_insurance_debt(rs.getBigDecimal("total_insurance_debt"));
        sale.setPayment_method(rs.getString("payment_method"));
        sale.setStatus(rs.getString("status"));
        
        return sale;
    }
    
    @Override
    public List<Sale> findByShiftId(int shiftId) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shiftId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مبيعات الوردية: " + e.getMessage());
        }
        return sales;
    }

    @Override
    public List<Sale> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales WHERE sale_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endDateTime = endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المبيعات ضمن النطاق الزمني: " + e.getMessage());
        }
        return sales;
    }

    @Override
    public List<Sale> findByCustomerId(int customerId) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales WHERE customer_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مبيعات الزبون: " + e.getMessage());
        }
        return sales;
    }

    @Override
    public List<Sale> findByInsuranceId(int insuranceId) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales WHERE insurance_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, insuranceId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مبيعات شركة التأمين: " + e.getMessage());
        }
        return sales;
    }

    @Override
    public List<Sale> findByStatus(String status) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales WHERE status = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المبيعات حسب الحالة: " + e.getMessage());
        }
        return sales;
    }

    @Override
    public List<Sale> findByPaymentMethod(String method) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM Sales WHERE payment_method = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, method);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المبيعات حسب طريقة الدفع: " + e.getMessage());
        }
        return sales;
    }
}
