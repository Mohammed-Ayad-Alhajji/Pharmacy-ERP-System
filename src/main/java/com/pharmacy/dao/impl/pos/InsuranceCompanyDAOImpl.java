// مسار الملف: src/main/java/com/pharmacy/dao/impl/pos/InsuranceCompanyDAOImpl.java

package com.pharmacy.dao.impl.pos;

import com.pharmacy.dao.interfaces.pos.InsuranceCompanyDAO;
import com.pharmacy.models.pos.InsuranceCompany;
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

public class InsuranceCompanyDAOImpl implements InsuranceCompanyDAO {

    @Override
    public Optional<InsuranceCompany> create(InsuranceCompany entity) {
        String sql = "INSERT INTO Insurance_Companies (name, contact_info, address) VALUES (?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getName());
            
            if (entity.getContact_info() != null) {
                pstmt.setString(2, entity.getContact_info());
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
                        entity.setInsurance_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إدراج شركة التأمين. قد يكون اسم الشركة مكرراً (UNIQUE Constraint): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<InsuranceCompany> findById(Integer id) {
        String sql = "SELECT * FROM Insurance_Companies WHERE insurance_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToInsuranceCompany(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن بيانات شركة التأمين: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<InsuranceCompany> findAll() {
        List<InsuranceCompany> companies = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Companies";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                companies.add(mapRowToInsuranceCompany(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة شركات التأمين: " + e.getMessage());
        }
        return companies;
    }

    @Override
    public List<InsuranceCompany> findAll(int limit, int offset) {
        List<InsuranceCompany> companies = new ArrayList<>();
        String sql = "SELECT * FROM Insurance_Companies LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    companies.add(mapRowToInsuranceCompany(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة شركات التأمين (Pagination): " + e.getMessage());
        }
        return companies;
    }

    @Override
    public boolean update(InsuranceCompany entity) {
        String sql = "UPDATE Insurance_Companies SET name = ?, contact_info = ?, address = ? WHERE insurance_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getName());
            
            if (entity.getContact_info() != null) {
                pstmt.setString(2, entity.getContact_info());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            if (entity.getAddress() != null) {
                pstmt.setString(3, entity.getAddress());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            
            pstmt.setInt(4, entity.getInsurance_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات شركة التأمين. تأكد من عدم تكرار الاسم (UNIQUE Constraint): " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Insurance_Companies WHERE insurance_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            // إذا كانت الشركة مرتبطة بفواتير، SQLite سيمنع الحذف بفضل ON DELETE RESTRICT
            System.err.println("[خطأ تنفيذي] فشل الحذف لارتباط شركة التأمين بسجلات مالية: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Insurance_Companies WHERE insurance_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود شركة التأمين: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Insurance_Companies";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي شركات التأمين: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<InsuranceCompany> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Insurance_Companies (name, contact_info, address) VALUES (?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (InsuranceCompany entity : entities) {
                    pstmt.setString(1, entity.getName());
                    
                    if (entity.getContact_info() != null) {
                        pstmt.setString(2, entity.getContact_info());
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
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع لشركات التأمين. قد يوجد تكرار في الأسماء، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء حفظ السجلات المجمعة: " + e.getMessage());
            return false;
        }
    }

    private InsuranceCompany mapRowToInsuranceCompany(ResultSet rs) throws SQLException {
        InsuranceCompany company = new InsuranceCompany();
        company.setInsurance_id(rs.getInt("insurance_id"));
        company.setName(rs.getString("name"));
        company.setContact_info(rs.getString("contact_info"));
        company.setAddress(rs.getString("address"));
        return company;
    }

    @Override
    public Optional<InsuranceCompany> findByName(String name) {
        String sql = "SELECT * FROM Insurance_Companies WHERE name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToInsuranceCompany(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن شركة التأمين بالاسم: " + e.getMessage());
        }
        return Optional.empty();
    }
    @Override
    public List<InsuranceCompany> searchByName(String query) {
        List<InsuranceCompany> companies = new ArrayList<>();
        // استخدمنا LIKE للبحث الجزئي، وتطابقنا مع اسم الجدول لديك
        String sql = "SELECT * FROM Insurance_Companies WHERE name LIKE ?";
        
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // إضافة علامات % للبحث عن أي نص يحتوي على الكلمة المدخلة
            pstmt.setString(1, "%" + query + "%");
            
            // استخدام try المتداخل لإغلاق الـ ResultSet بأمان (كما فعلت أنت)
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    companies.add(mapRowToInsuranceCompany(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل البحث الجزئي عن شركة التأمين: " + e.getMessage());
        }
        return companies;
    }
 
}