// src/main/java/com/pharmacy/dao/interfaces/inventory/MedicineDAO.java
package com.pharmacy.dao.interfaces.inventory;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.inventory.Medicine;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Medicine entity.
 * Updated for regulatory compliance.
 */
public interface MedicineDAO extends GenericDAO<Medicine, Integer> {
    
    // الدوال القديمة
    Optional<Medicine> findByBarcode(String barcode);
    
    List<Medicine> findByCategoryId(int categoryId);
    
    List<Medicine> findActiveMedicines();
    
    List<Medicine> findLowStockMedicines();
    
    List<Medicine> searchByName(String keyword);
    
    List<Medicine> findAlternatives(int medId);

    List<Medicine> findPrescriptionMedicines();
}