// مسار الملف: src/main/java/com/pharmacy/dao/impl/finance/ExpenseDAOImpl.java

package com.pharmacy.dao.impl.finance;

import com.pharmacy.dao.interfaces.finance.ExpenseDAO;
import com.pharmacy.models.finance.Expense;
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

public class ExpenseDAOImpl implements ExpenseDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Optional<Expense> create(Expense entity) {
        String sql = "INSERT INTO Expenses (category_id, shift_id, amount, expense_date, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entity.getCategory_id());
            pstmt.setInt(2, entity.getShift_id());
            pstmt.setBigDecimal(3, entity.getAmount());
            
            if (entity.getExpense_date() != null) {
                pstmt.setString(4, entity.getExpense_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setString(4, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }
            
            if (entity.getDescription() != null) {
                pstmt.setString(5, entity.getDescription());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setExpense_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج المصروف. تحقق من قيد المبلغ (amount > 0.0) وصحة المعرفات الأجنبية: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Expense> findById(Integer id) {
        String sql = "SELECT * FROM Expenses WHERE expense_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات المصروف: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Expense> findAll() {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM Expenses";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                expenses.add(mapRowToExpense(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة المصاريف: " + e.getMessage());
        }
        return expenses;
    }

    @Override
    public List<Expense> findAll(int limit, int offset) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM Expenses LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة المصاريف المجدولة: " + e.getMessage());
        }
        return expenses;
    }

    @Override
    public boolean update(Expense entity) {
        String sql = "UPDATE Expenses SET category_id = ?, shift_id = ?, amount = ?, expense_date = ?, description = ? WHERE expense_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, entity.getCategory_id());
            pstmt.setInt(2, entity.getShift_id());
            pstmt.setBigDecimal(3, entity.getAmount());
            
            if (entity.getExpense_date() != null) {
                pstmt.setString(4, entity.getExpense_date().format(DATE_TIME_FORMATTER));
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            
            if (entity.getDescription() != null) {
                pstmt.setString(5, entity.getDescription());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            pstmt.setInt(6, entity.getExpense_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات المصروف: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Expenses WHERE expense_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حذف المصروف: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Expenses WHERE expense_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود المصروف: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Expenses";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي المصاريف: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Expense> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Expenses (category_id, shift_id, amount, expense_date, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Expense entity : entities) {
                    pstmt.setInt(1, entity.getCategory_id());
                    pstmt.setInt(2, entity.getShift_id());
                    pstmt.setBigDecimal(3, entity.getAmount());
                    
                    if (entity.getExpense_date() != null) {
                        pstmt.setString(4, entity.getExpense_date().format(DATE_TIME_FORMATTER));
                    } else {
                        pstmt.setString(4, LocalDateTime.now().format(DATE_TIME_FORMATTER));
                    }
                    
                    if (entity.getDescription() != null) {
                        pstmt.setString(5, entity.getDescription());
                    } else {
                        pstmt.setNull(5, Types.VARCHAR);
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للمصاريف. تم التراجع (Rollback). تأكد من القيود المالية: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private Expense mapRowToExpense(ResultSet rs) throws SQLException {
        Expense expense = new Expense();
        expense.setExpense_id(rs.getInt("expense_id"));
        expense.setCategory_id(rs.getInt("category_id"));
        expense.setShift_id(rs.getInt("shift_id"));
        expense.setAmount(rs.getBigDecimal("amount"));
        
        String dateStr = rs.getString("expense_date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            expense.setExpense_date(LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER));
        }
        
        expense.setDescription(rs.getString("description"));
        
        return expense;
    }

    @Override
    public List<Expense> findByCategoryId(int categoryId) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM Expenses WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, categoryId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المصاريف حسب التصنيف: " + e.getMessage());
        }
        return expenses;
    }

    @Override
    public List<Expense> findByShiftId(int shiftId) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM Expenses WHERE shift_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, shiftId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن مصاريف الوردية: " + e.getMessage());
        }
        return expenses;
    }

    @Override
    public List<Expense> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM Expenses WHERE expense_date BETWEEN ? AND ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String startDateTime = startDate.atStartOfDay().format(DATE_TIME_FORMATTER);
            String endDateTime = endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER);
            
            pstmt.setString(1, startDateTime);
            pstmt.setString(2, endDateTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن المصاريف ضمن النطاق الزمني: " + e.getMessage());
        }
        return expenses;
    }
}