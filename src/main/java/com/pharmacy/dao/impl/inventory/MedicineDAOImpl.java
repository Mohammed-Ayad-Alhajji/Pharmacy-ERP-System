// مسار الملف: src/main/java/com/pharmacy/dao/impl/inventory/MedicineDAOImpl.java

package com.pharmacy.dao.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.MedicineDAO;
import com.pharmacy.models.inventory.Medicine;
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

public class MedicineDAOImpl implements MedicineDAO {

    @Override
    public Optional<Medicine> create(Medicine entity) {
        String sql = "INSERT INTO Medicines (barcode, brand_name, generic_name, category_id, dosage_form, conversion_factor, current_box_sell_price, current_unit_sell_price, prescription_required, min_stock_level, shelf_location, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, entity.getBarcode());
            pstmt.setString(2, entity.getBrand_name());
            pstmt.setString(3, entity.getGeneric_name());
            
            if (entity.getCategory_id() > 0) {
                pstmt.setInt(4, entity.getCategory_id());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.setString(5, entity.getDosage_form());
            pstmt.setInt(6, entity.getConversion_factor());
            pstmt.setBigDecimal(7, entity.getCurrent_box_sell_price());
            pstmt.setBigDecimal(8, entity.getCurrent_unit_sell_price());
            pstmt.setInt(9, entity.getPrescription_required());
            pstmt.setInt(10, entity.getMin_stock_level());
            pstmt.setString(11, entity.getShelf_location());
            pstmt.setInt(12, entity.getIs_active());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setMed_id(generatedKeys.getInt(1));
                        return Optional.of(entity);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إضافة الدواء: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Medicine> findById(Integer id) {
        String sql = "SELECT * FROM Medicines WHERE med_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدواء: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Medicine> findAll() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM Medicines";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                medicines.add(mapRowToMedicine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الأدوية: " + e.getMessage());
        }
        return medicines;
    }

    @Override
    public List<Medicine> findAll(int limit, int offset) {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM Medicines LIMIT ? OFFSET ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapRowToMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الأدوية (Pagination): " + e.getMessage());
        }
        return medicines;
    }

    @Override
    public boolean update(Medicine entity) {
        String sql = "UPDATE Medicines SET barcode = ?, brand_name = ?, generic_name = ?, category_id = ?, dosage_form = ?, conversion_factor = ?, current_box_sell_price = ?, current_unit_sell_price = ?, prescription_required = ?, min_stock_level = ?, shelf_location = ?, is_active = ? WHERE med_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entity.getBarcode());
            pstmt.setString(2, entity.getBrand_name());
            pstmt.setString(3, entity.getGeneric_name());
            
            if (entity.getCategory_id() > 0) {
                pstmt.setInt(4, entity.getCategory_id());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.setString(5, entity.getDosage_form());
            pstmt.setInt(6, entity.getConversion_factor());
            pstmt.setBigDecimal(7, entity.getCurrent_box_sell_price());
            pstmt.setBigDecimal(8, entity.getCurrent_unit_sell_price());
            pstmt.setInt(9, entity.getPrescription_required());
            pstmt.setInt(10, entity.getMin_stock_level());
            pstmt.setString(11, entity.getShelf_location());
            pstmt.setInt(12, entity.getIs_active());
            pstmt.setInt(13, entity.getMed_id());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل تحديث بيانات الدواء: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM Medicines WHERE med_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حذف الدواء. القيد (ON DELETE RESTRICT) يمنع الحذف الفعلي لوجود ارتباطات (مثل التشغيلات أو البدائل) بهذا الدواء: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Integer id) {
        String sql = "SELECT 1 FROM Medicines WHERE med_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الدواء: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM Medicines";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حساب إجمالي الأدوية: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean saveAll(List<Medicine> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Medicines (barcode, brand_name, generic_name, category_id, dosage_form, conversion_factor, current_box_sell_price, current_unit_sell_price, prescription_required, min_stock_level, shelf_location, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Medicine entity : entities) {
                    pstmt.setString(1, entity.getBarcode());
                    pstmt.setString(2, entity.getBrand_name());
                    pstmt.setString(3, entity.getGeneric_name());
                    
                    if (entity.getCategory_id() > 0) {
                        pstmt.setInt(4, entity.getCategory_id());
                    } else {
                        pstmt.setNull(4, Types.INTEGER);
                    }
                    
                    pstmt.setString(5, entity.getDosage_form());
                    pstmt.setInt(6, entity.getConversion_factor());
                    pstmt.setBigDecimal(7, entity.getCurrent_box_sell_price());
                    pstmt.setBigDecimal(8, entity.getCurrent_unit_sell_price());
                    pstmt.setInt(9, entity.getPrescription_required());
                    pstmt.setInt(10, entity.getMin_stock_level());
                    pstmt.setString(11, entity.getShelf_location());
                    pstmt.setInt(12, entity.getIs_active());
                    
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit(); 
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); 
                System.err.println("[خطأ تنفيذي] فشل الإدراج المجمع للأدوية، تم التراجع (Rollback): " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true); 
            }
            
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاتصال بقاعدة البيانات أثناء الإدراج المجمع: " + e.getMessage());
            return false;
        }
    }

    private Medicine mapRowToMedicine(ResultSet rs) throws SQLException {
        Medicine medicine = new Medicine();
        medicine.setMed_id(rs.getInt("med_id"));
        medicine.setBarcode(rs.getString("barcode"));
        medicine.setBrand_name(rs.getString("brand_name"));
        medicine.setGeneric_name(rs.getString("generic_name"));
        
        int categoryId = rs.getInt("category_id");
        if (!rs.wasNull()) {
            medicine.setCategory_id(categoryId);
        }
        
        medicine.setDosage_form(rs.getString("dosage_form"));
        medicine.setConversion_factor(rs.getInt("conversion_factor"));
        medicine.setCurrent_box_sell_price(rs.getBigDecimal("current_box_sell_price"));
        medicine.setCurrent_unit_sell_price(rs.getBigDecimal("current_unit_sell_price"));
        medicine.setPrescription_required(rs.getInt("prescription_required"));
        medicine.setMin_stock_level(rs.getInt("min_stock_level"));
        medicine.setShelf_location(rs.getString("shelf_location"));
        medicine.setIs_active(rs.getInt("is_active"));
        
        return medicine;
    }

    @Override
    public Optional<Medicine> findByBarcode(String barcode) {
        // السر هنا: يجب أن يكون البحث في عمود barcode
        String sql = "SELECT * FROM Medicines WHERE barcode = ?";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل الاستعلام عن الدواء بواسطة الباركود: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Medicine> findByCategoryId(int categoryId) {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM Medicines WHERE category_id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, categoryId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapRowToMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب الأدوية المرتبطة بالتصنيف: " + e.getMessage());
        }
        return medicines;
    }

    @Override
    public List<Medicine> findActiveMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM Medicines WHERE is_active = 1";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                medicines.add(mapRowToMedicine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب قائمة الأدوية الفعالة: " + e.getMessage());
        }
        return medicines;
    }

    @Override
    public List<Medicine> findLowStockMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        // استخدام الاستعلام الإلزامي المعماري لدمج الكميات مع معامل التحويل
        String sql = "SELECT m.* FROM Medicines m JOIN Batches b ON m.med_id = b.med_id WHERE m.is_active = 1 AND b.is_active = 1 GROUP BY m.med_id HAVING SUM(b.quantity / m.conversion_factor) <= m.min_stock_level";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                medicines.add(mapRowToMedicine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل استخراج تقرير نواقص الأدوية: " + e.getMessage());
        }
        return medicines;
    }

    
    
    public List<Medicine> searchByName(String keyword) {
        List<Medicine> medicines = new ArrayList<>();
        // السر هنا: إضافة البحث بالباركود إلى جانب الاسم
        String sql = "SELECT * FROM Medicines WHERE barcode LIKE ? OR brand_name LIKE ? OR generic_name LIKE ?";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapRowToMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل البحث عن الدواء: " + e.getMessage());
        }
        return medicines;
    }
    @Override
    public List<Medicine> findAlternatives(int medId) {
        List<Medicine> alternatives = new ArrayList<>();
        // استخدام UNION لتغطية جهتي المفتاح في جدول الربط مع تجنب مشاكل الأداء الخاصة بـ OR
        String sql = "SELECT m.* FROM Medicines m JOIN Drug_Alternatives da ON m.med_id = da.med_id_2 WHERE da.med_id_1 = ? " +
                     "UNION " +
                     "SELECT m.* FROM Medicines m JOIN Drug_Alternatives da ON m.med_id = da.med_id_1 WHERE da.med_id_2 = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, medId);
            pstmt.setInt(2, medId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    alternatives.add(mapRowToMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب بدائل الدواء: " + e.getMessage());
        }
        return alternatives;
    }

    @Override
    public List<Medicine> findPrescriptionMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM Medicines WHERE prescription_required = 1";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                medicines.add(mapRowToMedicine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب أدوية الوصفات الطبية: " + e.getMessage());
        }
        return medicines;
    }
}