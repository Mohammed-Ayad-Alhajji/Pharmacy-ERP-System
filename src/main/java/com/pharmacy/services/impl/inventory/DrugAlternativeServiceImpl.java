// مسار الملف: src/main/java/com/pharmacy/services/impl/inventory/DrugAlternativeServiceImpl.java

package com.pharmacy.services.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.DrugAlternativeDAO;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.services.interfaces.inventory.DrugAlternativeService;
import java.util.List;

/**
 * تنفيذ خدمة البدائل الدوائية.
 * يتم التحقق من قواعد العمل (Business Rules) قبل تمرير الاستعلام لطبقة الـ DAO.
 */
public class DrugAlternativeServiceImpl implements DrugAlternativeService {

    private final DrugAlternativeDAO drugAlternativeDAO;

    /**
     * حقن الاعتمادية عبر الـ Constructor لضمان قابلية الاختبار والفصل المعماري.
     * @param drugAlternativeDAO واجهة الوصول للبيانات الخاصة بالبدائل.
     */
    public DrugAlternativeServiceImpl(DrugAlternativeDAO drugAlternativeDAO) {
        this.drugAlternativeDAO = drugAlternativeDAO;
    }

    @Override
    public boolean addAlternative(int medId1, int medId2) {
        // 1. التحقق من صحة المعرفات
        if (medId1 <= 0 || medId2 <= 0) {
            throw new IllegalArgumentException("أرقام الأدوية غير صالحة");
        }

        // 2. التحقق من المنطق: لا يمكن للدواء أن يكون بديلاً لنفسه
        if (medId1 == medId2) {
            throw new IllegalArgumentException("لا يمكن أن يكون الدواء بديلاً لنفسه");
        }

        // 3. التحقق من التكرار في قاعدة البيانات
        if (drugAlternativeDAO.exists(medId1, medId2)) {
            throw new IllegalArgumentException("هذا البديل مضاف مسبقاً لهذا الدواء");
        }

        // 4. تنفيذ عملية الإضافة
        return drugAlternativeDAO.addAlternative(medId1, medId2);
    }

    @Override
    public boolean removeAlternative(int medId1, int medId2) {
        // تمرير مباشر لطبقة الـ DAO لإزالة العلاقة بين الدوائين
        return drugAlternativeDAO.removeAlternative(medId1, medId2);
    }

    @Override
    public boolean checkAlternativeExists(int medId1, int medId2) {
        // فحص وجود علاقة بديل في جدول الوصل
        return drugAlternativeDAO.exists(medId1, medId2);
    }
    
    @Override
    public List<Medicine> getAlternatives(int medId) {
        if (medId <= 0) {
            return new java.util.ArrayList<>();
        }
        return drugAlternativeDAO.getAlternatives(medId);
    }
}