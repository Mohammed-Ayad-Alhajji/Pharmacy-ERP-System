// مسار الملف: src/main/java/com/pharmacy/dao/impl/inventory/CategoryDAOImpl.java

package com.pharmacy.dao.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.CategoryDAO;
import com.pharmacy.models.inventory.Category;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDAOImpl implements CategoryDAO {

    @Override
    public Optional<Category> create(Category entity) {
        String sql = "INSERT INTO Categories (name, description) VALUES (?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getDescription());
            
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
            System.err.println("[خطأ تنفيذي] فشل الإضافة. قد يكون اسم التصنيف مكرراً (UNIQUE Constraint): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Category> findById(Integer id) {
        String sql = "SELECT * FROM Categories WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن التصنيف: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Category> findAll() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM Categories";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(mapRowToCategory(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة التصنيفات: " + e.getMessage());
        }
        return categories;
    }

    @Override
    public List<Category> findAll(int limit, int offset) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM Categories LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة التصنيفات (Pagination): " + e.getMessage());
        }
        return categories;
    }

    @Override
    public boolean update(Category entity) {
        String sql = "UPDATE Categories SET name = ?, description = ? WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getDescription());
            pstmt.setInt(3, entity.getCategory_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحديث. قد يكون اسم التصنيف مكرراً (UNIQUE Constraint): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Categories WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الحذف. القيد (ON DELETE RESTRICT) يمنع الحذف لوجود أدوية مرتبطة بهذا التصنيف: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Categories WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود التصنيف: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Categories";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب عدد التصنيفات: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Category> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Categories (name, description) VALUES (?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); // بدء الـ Transaction
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Category entity : entities) {
                    pstmt.setString(1, entity.getName());
                    pstmt.setString(2, entity.getDescription());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); // تأكيد الـ Transaction
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); // التراجع في حال الفشل
                System.err.println("[خطأ تنفيذي] فشل إدراج الدفعة، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); // إعادة الضبط الافتراضي
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ الدفعة: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Category> findByName(String name) {
        String sql = "SELECT * FROM Categories WHERE name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل البحث بالاسم: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Category mapRowToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategory_id(rs.getInt("category_id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        return category;
    }
}