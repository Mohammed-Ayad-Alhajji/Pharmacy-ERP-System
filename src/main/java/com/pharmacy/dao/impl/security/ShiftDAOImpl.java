// مسار الملف: src/main/java/com/pharmacy/dao/impl/security/ShiftDAOImpl.java

package com.pharmacy.dao.impl.security;

import com.pharmacy.dao.interfaces.security.ShiftDAO;
import com.pharmacy.models.finance.ShiftFinancialSummary;
import com.pharmacy.models.security.Shift;
import com.pharmacy.utils.DBConnectionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShiftDAOImpl implements ShiftDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<Shift> create(Shift entity) {
        String sql = "INSERT INTO Shifts (user_id, start_time, end_time, opening_balance, expected_closing_balance, actual_closing_balance, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getUser_id());
            
            if (entity.getStart_time() != null) {
                pstmt.setString(2, entity.getStart_time().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getEnd_time() != null) {
                pstmt.setString(3, entity.getEnd_time().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            pstmt.setBigDecimal(4, entity.getOpening_balance());
            pstmt.setBigDecimal(5, entity.getExpected_closing_balance());
            pstmt.setBigDecimal(6, entity.getActual_closing_balance());
            
            if (entity.getStatus() != null) {
                pstmt.setString(7, entity.getStatus());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setShift_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج الوردية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Shift> findById(Integer id) {
        String sql = "SELECT * FROM Shifts WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToShift(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الوردية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Shift> findAll() {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM Shifts";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                shifts.add(mapRowToShift(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة الورديات: " + e.getMessage());
        }
        return shifts;
    }

    @Override
    public List<Shift> findAll(int limit, int offset) {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM Shifts LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRowToShift(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب الورديات المجدولة: " + e.getMessage());
        }
        return shifts;
    }

    @Override
    public boolean update(Shift entity) {
        String sql = "UPDATE Shifts SET user_id = ?, start_time = ?, end_time = ?, opening_balance = ?, expected_closing_balance = ?, actual_closing_balance = ?, status = ? WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getUser_id());
            
            if (entity.getStart_time() != null) {
                pstmt.setString(2, entity.getStart_time().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getEnd_time() != null) {
                pstmt.setString(3, entity.getEnd_time().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            pstmt.setBigDecimal(4, entity.getOpening_balance());
            pstmt.setBigDecimal(5, entity.getExpected_closing_balance());
            pstmt.setBigDecimal(6, entity.getActual_closing_balance());
            
            if (entity.getStatus() != null) {
                pstmt.setString(7, entity.getStatus());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            pstmt.setInt(8, entity.getShift_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات الوردية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        System.err.println("إجراء محظور: يُمنع حذف الورديات للحفاظ على الميزانية العامة وتتبع الفواتير");
        return false;
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Shifts WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الوردية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Shifts";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الورديات: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Shift> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Shifts (user_id, start_time, end_time, opening_balance, expected_closing_balance, actual_closing_balance, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Shift entity : entities) {
                    pstmt.setInt(1, entity.getUser_id());
                    
                    if (entity.getStart_time() != null) {
                        pstmt.setString(2, entity.getStart_time().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setNull(2, Types.VARCHAR);
                    }
                    
                    if (entity.getEnd_time() != null) {
                        pstmt.setString(3, entity.getEnd_time().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setNull(3, Types.VARCHAR);
                    }
                    
                    pstmt.setBigDecimal(4, entity.getOpening_balance());
                    pstmt.setBigDecimal(5, entity.getExpected_closing_balance());
                    pstmt.setBigDecimal(6, entity.getActual_closing_balance());
                    
                    if (entity.getStatus() != null) {
                        pstmt.setString(7, entity.getStatus());
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
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للورديات. تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private Shift mapRowToShift(ResultSet rs) throws SQLException {
        Shift shift = new Shift();
        shift.setShift_id(rs.getInt("shift_id"));
        shift.setUser_id(rs.getInt("user_id"));
        
        String startStr = rs.getString("start_time");
        if (startStr != null && !startStr.trim().isEmpty()) {
            shift.setStart_time(LocalDateTime.parse(startStr, DATE_TIME_FORMATTER));
        }
        
        String endStr = rs.getString("end_time");
        if (endStr != null && !endStr.trim().isEmpty()) {
            shift.setEnd_time(LocalDateTime.parse(endStr, DATE_TIME_FORMATTER));
        }
        
        shift.setOpening_balance(rs.getBigDecimal("opening_balance"));
        shift.setExpected_closing_balance(rs.getBigDecimal("expected_closing_balance"));
        shift.setActual_closing_balance(rs.getBigDecimal("actual_closing_balance"));
        shift.setStatus(rs.getString("status"));
        
        return shift;
    }

    @Override
    public Optional<Shift> findOpenShiftByUserId(int userId) {
        String sql = "SELECT * FROM Shifts WHERE user_id = ? AND status = 'Open'";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToShift(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الوردية المفتوحة للمستخدم: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Shift> findByUserId(int userId) {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM Shifts WHERE user_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRowToShift(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن ورديات المستخدم: " + e.getMessage());
        }
        return shifts;
    }

    @Override
    public List<Shift> findByStatus(String status) {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM Shifts WHERE status = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRowToShift(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الورديات حسب الحالة: " + e.getMessage());
        }
        return shifts;
    }

    @Override
    public List<Shift> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM Shifts WHERE start_time BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startDate.format(DATE_TIME_FORMATTER));
            pstmt.setString(2, endDate.format(DATE_TIME_FORMATTER));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRowToShift(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الورديات ضمن النطاق الزمني: " + e.getMessage());
        }
        return shifts;
    }

    @Override
    public List<Shift> findByUserIdAndDateRange(int userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM Shifts WHERE user_id = ? AND start_time BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, startDate.format(DATE_TIME_FORMATTER));
            pstmt.setString(3, endDate.format(DATE_TIME_FORMATTER));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRowToShift(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن ورديات المستخدم ضمن النطاق الزمني: " + e.getMessage());
        }
        return shifts;
    }
    
    @Override
    public Optional<Shift> getShiftById(int shiftId) {
        String sql = "SELECT * FROM Shifts WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, shiftId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Shift shift = new Shift();
                    shift.setShift_id(rs.getInt("shift_id"));
                    shift.setUser_id(rs.getInt("user_id"));
                    // إذا كنت تستخدم الحقول الأخرى يمكنك إضافتها هنا
                    // shift.setStart_time(rs.getTimestamp("start_time").toLocalDateTime());
                    return Optional.of(shift);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    // تأكد من عمل import لـ com.pharmacy.models.finance.ShiftFinancialSummary;
    
    public ShiftFinancialSummary getShiftFinancialDetails(int shiftId) {
        ShiftFinancialSummary summary = new ShiftFinancialSummary();
        summary.shiftId = shiftId;

        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            
            // 1. الرصيد الافتتاحي والفعلي من جدول الورديات
            try (PreparedStatement ps = conn.prepareStatement("SELECT opening_balance, actual_closing_balance FROM Shifts WHERE shift_id = ?")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    summary.openingBalance = rs.getBigDecimal("opening_balance") != null ? rs.getBigDecimal("opening_balance") : BigDecimal.ZERO;
                    summary.actualBalance = rs.getBigDecimal("actual_closing_balance") != null ? rs.getBigDecimal("actual_closing_balance") : BigDecimal.ZERO;
                }
            }

            // 2. (+) المبيعات النقدية (ما دفعه المريض فوراً في الفواتير)
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(total_patient_paid) FROM Sales WHERE shift_id = ?")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBigDecimal(1) != null) summary.cashSales = rs.getBigDecimal(1);
            }

            // 3. (+) مقبوضات العملاء (سداد الديون)
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount_paid) FROM Customer_Payments WHERE shift_id = ?")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBigDecimal(1) != null) summary.customerReceipts = rs.getBigDecimal(1);
            }

            // 4. (+) مقبوضات التأمين
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount_paid) FROM Insurance_Payments WHERE shift_id = ?")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBigDecimal(1) != null) summary.insuranceReceipts = rs.getBigDecimal(1);
            }

            // 5. (+) استرداد نقدي من الموردين (مرتجعات الموردين المدفوعة كاش)
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount_paid) FROM Supplier_Payments WHERE shift_id = ? AND transaction_type = 'Refund'")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBigDecimal(1) != null) summary.supplierRefunds = rs.getBigDecimal(1);
            }

            // 6. (-) مرتجعات المرضى (ما دفعناه للمريض كاش)
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(patient_cash_refund) FROM Patient_Returns WHERE shift_id = ?")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBigDecimal(1) != null) summary.patientRefunds = rs.getBigDecimal(1);
            }

            // 7. (-) مدفوعات الموردين (ما صرفناه للمندوبين)
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount_paid) FROM Supplier_Payments WHERE shift_id = ? AND transaction_type = 'Payment'")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBigDecimal(1) != null) summary.supplierPayments = rs.getBigDecimal(1);
            }

            // 8. (-) المصروفات اليومية
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount) FROM Expenses WHERE shift_id = ?")) {
                ps.setInt(1, shiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBigDecimal(1) != null) summary.expenses = rs.getBigDecimal(1);
            }

            // --- 9. حساب الرصيد المتوقع (Expected Balance) ---
            summary.expectedBalance = summary.openingBalance
                    .add(summary.cashSales)
                    .add(summary.customerReceipts)
                    .add(summary.insuranceReceipts)
                    .add(summary.supplierRefunds)
                    .subtract(summary.patientRefunds)
                    .subtract(summary.supplierPayments)
                    .subtract(summary.expenses);

            // --- 10. حساب العجز أو الزيادة ---
            summary.difference = summary.actualBalance.subtract(summary.expectedBalance);

        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب تفاصيل الوردية: " + e.getMessage());
        }

        return summary;
    }
}