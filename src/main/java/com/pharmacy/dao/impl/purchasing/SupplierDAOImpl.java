// مسار الملف: src/main/java/com/pharmacy/dao/impl/purchasing/SupplierDAOImpl.java

package com.pharmacy.dao.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.SupplierDAO;
import com.pharmacy.models.purchasing.Supplier;
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

public class SupplierDAOImpl implements SupplierDAO {

    @Override
    public Optional<Supplier> create(Supplier entity) {
        String sql = "INSERT INTO Suppliers (name, contact_person, phone, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getName());
            
            if (entity.getContact_person() != null) {
                pstmt.setString(2, entity.getContact_person());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getPhone() != null) {
                pstmt.setString(3, entity.getPhone());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            if (entity.getAddress() != null) {
                pstmt.setString(4, entity.getAddress());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setSupplier_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج المورد. قد يكون اسم المورد مكرراً (UNIQUE Constraint): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Supplier> findById(Integer id) {
        String sql = "SELECT * FROM Suppliers WHERE supplier_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSupplier(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات المورد: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Supplier> findAll() {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT * FROM Suppliers";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                suppliers.add(mapRowToSupplier(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الموردين: " + e.getMessage());
        }
        return suppliers;
    }

    @Override
    public List<Supplier> findAll(int limit, int offset) {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT * FROM Suppliers LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    suppliers.add(mapRowToSupplier(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الموردين (Pagination): " + e.getMessage());
        }
        return suppliers;
    }

    @Override
    public boolean update(Supplier entity) {
        String sql = "UPDATE Suppliers SET name = ?, contact_person = ?, phone = ?, address = ? WHERE supplier_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getName());
            
            if (entity.getContact_person() != null) {
                pstmt.setString(2, entity.getContact_person());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getPhone() != null) {
                pstmt.setString(3, entity.getPhone());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            if (entity.getAddress() != null) {
                pstmt.setString(4, entity.getAddress());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            
            pstmt.setInt(5, entity.getSupplier_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات المورد. تأكد من عدم تكرار الاسم (UNIQUE Constraint): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Suppliers WHERE supplier_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حذف المورد. القيد (ON DELETE RESTRICT) يمنع الحذف لارتباطه بفواتير، مرتجعات، أو دفعات مالية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Suppliers WHERE supplier_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود المورد: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Suppliers";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الموردين: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Supplier> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Suppliers (name, contact_person, phone, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Supplier entity : entities) {
                    pstmt.setString(1, entity.getName());
                    
                    if (entity.getContact_person() != null) {
                        pstmt.setString(2, entity.getContact_person());
                    } else {
                        pstmt.setNull(2, Types.VARCHAR);
                    }
                    
                    if (entity.getPhone() != null) {
                        pstmt.setString(3, entity.getPhone());
                    } else {
                        pstmt.setNull(3, Types.VARCHAR);
                    }
                    
                    if (entity.getAddress() != null) {
                        pstmt.setString(4, entity.getAddress());
                    } else {
                        pstmt.setNull(4, Types.VARCHAR);
                    }
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للموردين. قد يوجد تكرار في الأسماء، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع للموردين: " + e.getMessage());
            return false;
        }
    }

    private Supplier mapRowToSupplier(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setSupplier_id(rs.getInt("supplier_id"));
        supplier.setName(rs.getString("name"));
        supplier.setContact_person(rs.getString("contact_person"));
        supplier.setPhone(rs.getString("phone"));
        supplier.setAddress(rs.getString("address"));
        return supplier;
    }

    @Override
    public Optional<Supplier> findByName(String name) {
        String sql = "SELECT * FROM Suppliers WHERE name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSupplier(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل البحث عن المورد بالاسم: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Supplier> search(String keyword) {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT * FROM Suppliers WHERE name LIKE ? OR contact_person LIKE ? OR phone LIKE ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    suppliers.add(mapRowToSupplier(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل البحث المرن عن الموردين: " + e.getMessage());
        }
        return suppliers;
    }
}