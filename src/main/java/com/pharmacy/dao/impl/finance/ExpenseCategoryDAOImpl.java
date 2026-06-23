// مسار الملف: src/main/java/com/pharmacy/dao/impl/finance/ExpenseCategoryDAOImpl.java

package com.pharmacy.dao.impl.finance;

import com.pharmacy.dao.interfaces.finance.ExpenseCategoryDAO;
import com.pharmacy.models.finance.ExpenseCategory;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpenseCategoryDAOImpl implements ExpenseCategoryDAO {

    @Override
    public Optional<ExpenseCategory> create(ExpenseCategory entity) {
        String sql = "INSERT INTO Expense_Categories (name) VALUES (?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getName());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setCategory_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج فئة المصاريف. قد يكون الاسم مكرراً (UNIQUE Constraint): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<ExpenseCategory> findById(Integer id) {
        String sql = "SELECT * FROM Expense_Categories WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToExpenseCategory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات فئة المصاريف: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<ExpenseCategory> findAll() {
        List<ExpenseCategory> categories = new ArrayList<>();
        String sql = "SELECT * FROM Expense_Categories";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(mapRowToExpenseCategory(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب كافة فئات المصاريف: " + e.getMessage());
        }
        return categories;
    }

    @Override
    public List<ExpenseCategory> findAll(int limit, int offset) {
        List<ExpenseCategory> categories = new ArrayList<>();
        String sql = "SELECT * FROM Expense_Categories LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapRowToExpenseCategory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب فئات المصاريف المجدولة: " + e.getMessage());
        }
        return categories;
    }

    @Override
    public boolean update(ExpenseCategory entity) {
        String sql = "UPDATE Expense_Categories SET name = ? WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getName());
            pstmt.setInt(2, entity.getCategory_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات فئة المصاريف. تأكد من عدم تكرار الاسم (UNIQUE Constraint): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Expense_Categories WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            // إذا كان هناك مصروفات مرتبطة بهذه الفئة، سيرمي SQLite خطأ (Constraint Violation)
            System.err.println("[خطأ تنفيذي] لا يمكن حذف الفئة لارتباطها بسجلات أخرى: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Expense_Categories WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود فئة المصاريف: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Expense_Categories";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي فئات المصاريف: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<ExpenseCategory> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Expense_Categories (name) VALUES (?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (ExpenseCategory entity : entities) {
                    pstmt.setString(1, entity.getName());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لفئات المصاريف. تم التراجع (Rollback). قد يوجد تكرار في الأسماء: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<ExpenseCategory> findByName(String name) {
        String sql = "SELECT * FROM Expense_Categories WHERE name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToExpenseCategory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن فئة المصاريف بالاسم: " + e.getMessage());
        }
        return Optional.empty();
    }

    private ExpenseCategory mapRowToExpenseCategory(ResultSet rs) throws SQLException {
        ExpenseCategory category = new ExpenseCategory();
        category.setCategory_id(rs.getInt("category_id"));
        category.setName(rs.getString("name"));
        return category;
    }
}