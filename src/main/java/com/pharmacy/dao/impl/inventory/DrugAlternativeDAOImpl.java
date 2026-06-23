package com.pharmacy.dao.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.DrugAlternativeDAO;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.utils.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DrugAlternativeDAOImpl implements DrugAlternativeDAO {

    @Override
    public boolean addAlternative(int medId1, int medId2) {
        // منع المرجعية الذاتية
        if (medId1 == medId2) {
            return false;
        }

        // منع التكرار المزدوج (الترتيب)
        int id1 = Math.min(medId1, medId2);
        int id2 = Math.max(medId1, medId2);

        String sql = "INSERT OR IGNORE INTO Drug_Alternatives (med_id_1, med_id_2) VALUES (?, ?)";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id1);
            pstmt.setInt(2, id2);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل إضافة الدواء البديل: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeAlternative(int medId1, int medId2) {
        // منع المرجعية الذاتية
        if (medId1 == medId2) {
            return false;
        }

        // منع التكرار المزدوج (الترتيب)
        int id1 = Math.min(medId1, medId2);
        int id2 = Math.max(medId1, medId2);

        String sql = "DELETE FROM Drug_Alternatives WHERE med_id_1 = ? AND med_id_2 = ?";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id1);
            pstmt.setInt(2, id2);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل حذف الدواء البديل: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(int medId1, int medId2) {
        // منع المرجعية الذاتية
        if (medId1 == medId2) {
            return false;
        }

        // منع التكرار المزدوج (الترتيب)
        int id1 = Math.min(medId1, medId2);
        int id2 = Math.max(medId1, medId2);

        String sql = "SELECT 1 FROM Drug_Alternatives WHERE med_id_1 = ? AND med_id_2 = ?";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id1);
            pstmt.setInt(2, id2);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل التحقق من وجود الدواء البديل: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<Medicine> getAlternatives(int medId) {
        List<Medicine> alternatives = new java.util.ArrayList<>();
        String sql = "SELECT m.* FROM Medicines m " +
                     "JOIN Drug_Alternatives da " +
                     "ON (m.med_id = da.med_id_1 OR m.med_id = da.med_id_2) " +
                     "WHERE (da.med_id_1 = ? OR da.med_id_2 = ?) AND m.med_id != ?";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, medId);
            pstmt.setInt(2, medId);
            pstmt.setInt(3, medId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Medicine med = new Medicine();
                    med.setMed_id(rs.getInt("med_id"));
                    med.setBarcode(rs.getString("barcode"));
                    med.setBrand_name(rs.getString("brand_name"));
                    med.setGeneric_name(rs.getString("generic_name"));
                    med.setCategory_id(rs.getInt("category_id"));
                    // يمكنك جلب باقي الحقول هنا إن أردت، ولكن هذه تكفي للعرض في جدول البدائل
                    alternatives.add(med);
                }
            }
        } catch (SQLException e) {
            System.err.println("[خطأ تنفيذي] فشل جلب البدائل الدوائية: " + e.getMessage());
        }
        return alternatives;
    }
}