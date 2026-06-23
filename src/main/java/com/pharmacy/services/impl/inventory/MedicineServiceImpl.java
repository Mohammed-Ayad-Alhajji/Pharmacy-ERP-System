// مسار الملف: src/main/java/com/pharmacy/services/impl/inventory/MedicineServiceImpl.java

package com.pharmacy.services.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.MedicineDAO;
import com.pharmacy.models.inventory.Medicine;
import com.pharmacy.services.interfaces.inventory.MedicineService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class MedicineServiceImpl implements MedicineService {

    private final MedicineDAO medicineDAO;

    public MedicineServiceImpl(MedicineDAO medicineDAO) {
        this.medicineDAO = medicineDAO;
    }

    private void validateMedicine(Medicine medicine) {
        if (medicine.getBrand_name() == null || medicine.getBrand_name().trim().isEmpty() ||
            medicine.getGeneric_name() == null || medicine.getGeneric_name().trim().isEmpty()) {
            throw new IllegalArgumentException("الاسم التجاري والعلمي مطلوبان");
        }

        if (medicine.getConversion_factor() <= 0) {
            throw new IllegalArgumentException("عامل التحويل يجب أن يكون أكبر من الصفر");
        }

        if (medicine.getCurrent_box_sell_price() == null || medicine.getCurrent_box_sell_price().compareTo(BigDecimal.ZERO) < 0 ||
            medicine.getCurrent_unit_sell_price() == null || medicine.getCurrent_unit_sell_price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("أسعار البيع غير صالحة");
        }
    }

    @Override
    public Optional<Medicine> createMedicine(Medicine medicine) {
        validateMedicine(medicine);

        if (medicine.getBarcode() != null && !medicine.getBarcode().trim().isEmpty()) {
            Optional<Medicine> existing = medicineDAO.findByBarcode(medicine.getBarcode());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("يوجد دواء آخر يحمل نفس الباركود");
            }
        }

        return medicineDAO.create(medicine);
    }

    @Override
    public boolean updateMedicine(Medicine medicine) {
        validateMedicine(medicine);

        if (medicine.getBarcode() != null && !medicine.getBarcode().trim().isEmpty()) {
            Optional<Medicine> existing = medicineDAO.findByBarcode(medicine.getBarcode());
            if (existing.isPresent() && existing.get().getMed_id() != medicine.getMed_id()) {
                throw new IllegalArgumentException("يوجد دواء آخر يحمل نفس الباركود");
            }
        }

        return medicineDAO.update(medicine);
    }

    @Override
    public boolean deleteMedicine(int id) {
        try {
            boolean isDeleted = medicineDAO.delete(id);
            if (!isDeleted) {
                throw new IllegalStateException("لا يمكن حذف هذا الدواء لارتباطه بعمليات أخرى. يرجى إيقاف تفعيله بدلاً من ذلك.");
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("لا يمكن حذف هذا الدواء لارتباطه بعمليات أخرى. يرجى إيقاف تفعيله بدلاً من ذلك.", e);
        }
    }

    @Override
    public boolean deactivateMedicine(int id) {
        Optional<Medicine> optMedicine = medicineDAO.findById(id);
        if (optMedicine.isPresent()) {
            Medicine medicine = optMedicine.get();
            medicine.setIs_active(0);
            return medicineDAO.update(medicine);
        }
        return false;
    }

    @Override
    public Optional<Medicine> getMedicineById(int id) {
        return medicineDAO.findById(id);
    }

    @Override
    public Optional<Medicine> getMedicineByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return Optional.empty();
        }
        return medicineDAO.findByBarcode(barcode);
    }

    @Override
    public List<Medicine> searchMedicines(String keyword) {
        return medicineDAO.searchByName(keyword); 
    }

    @Override
    public List<Medicine> getActiveMedicines() {
        return medicineDAO.findActiveMedicines();
    }

    @Override
    public List<Medicine> getLowStockMedicines() {
        return medicineDAO.findLowStockMedicines();
    }

    @Override
    public List<Medicine> getMedicinesByCategory(int categoryId) {
        return medicineDAO.findByCategoryId(categoryId);
    }
    
    @Override
    public List<Medicine> getAllMedicines() {
        return medicineDAO.findAll(); 
    }
}