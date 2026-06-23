// مسار الملف: src/main/java/com/pharmacy/dao/impl/pos/LocalCustomerDAOImpl.java

package com.pharmacy.dao.impl.pos;

import com.pharmacy.dao.interfaces.pos.LocalCustomerDAO;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocalCustomerDAOImpl implements LocalCustomerDAO {

    @Override
    public Optional<LocalCustomer> create(LocalCustomer entity) {
        String sql = "INSERT INTO Local_Customers (name, phone, address) VALUES (?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getName());
            
            if (entity.getPhone() != null) {
                pstmt.setString(2, entity.getPhone());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getAddress() != null) {
                pstmt.setString(3, entity.getAddress());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setCustomer_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج الزبون: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<LocalCustomer> findById(Integer id) {
        String sql = "SELECT * FROM Local_Customers WHERE customer_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToLocalCustomer(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات الزبون: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<LocalCustomer> findAll() {
        List<LocalCustomer> customers = new ArrayList<>();
        String sql = "SELECT * FROM Local_Customers";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                customers.add(mapRowToLocalCustomer(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الزبائن: " + e.getMessage());
        }
        return customers;
    }

    @Override
    public List<LocalCustomer> findAll(int limit, int offset) {
        List<LocalCustomer> customers = new ArrayList<>();
        String sql = "SELECT * FROM Local_Customers LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapRowToLocalCustomer(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الزبائن المجدولة (Pagination): " + e.getMessage());
        }
        return customers;
    }

    @Override
    public boolean update(LocalCustomer entity) {
        String sql = "UPDATE Local_Customers SET name = ?, phone = ?, address = ? WHERE customer_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getName());
            
            if (entity.getPhone() != null) {
                pstmt.setString(2, entity.getPhone());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getAddress() != null) {
                pstmt.setString(3, entity.getAddress());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            pstmt.setInt(4, entity.getCustomer_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات الزبون: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Local_Customers WHERE customer_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            // إذا كان العميل مرتبطاً بعمليات مالية، سيرفض SQLite الحذف ويرمي خطأ قيد المفتاح الأجنبي
            System.err.println("[خطأ تنفيذي] فشل حذف العميل (مرتبط بعمليات مالية): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Local_Customers WHERE customer_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الزبون: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Local_Customers";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الزبائن: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<LocalCustomer> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Local_Customers (name, phone, address) VALUES (?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (LocalCustomer entity : entities) {
                    pstmt.setString(1, entity.getName());
                    
                    if (entity.getPhone() != null) {
                        pstmt.setString(2, entity.getPhone());
                    } else {
                        pstmt.setNull(2, Types.VARCHAR);
                    }
                    
                    if (entity.getAddress() != null) {
                        pstmt.setString(3, entity.getAddress());
                    } else {
                        pstmt.setNull(3, Types.VARCHAR);
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للزبائن، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ السجلات المجمعة: " + e.getMessage());
            return false;
        }
    }

    private LocalCustomer mapRowToLocalCustomer(ResultSet rs) throws SQLException {
        LocalCustomer customer = new LocalCustomer();
        customer.setCustomer_id(rs.getInt("customer_id"));
        customer.setName(rs.getString("name"));
        customer.setPhone(rs.getString("phone"));
        customer.setAddress(rs.getString("address"));
        return customer;
    }

    @Override
    public List<LocalCustomer> searchByNameOrPhone(String keyword) {
        List<LocalCustomer> customers = new ArrayList<>();
        String sql = "SELECT * FROM Local_Customers WHERE name LIKE ? OR phone LIKE ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapRowToLocalCustomer(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل البحث عن الزبائن بالاسم أو الرقم: " + e.getMessage());
        }
        return customers;
    }
    
    
}