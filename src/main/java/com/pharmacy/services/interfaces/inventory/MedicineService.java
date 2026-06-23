// مسار الملف: src/main/java/com/pharmacy/services/interfaces/inventory/MedicineService.java

package com.pharmacy.services.interfaces.inventory;

import com.pharmacy.models.inventory.Medicine;

import java.util.List;
import java.util.Optional;

public interface MedicineService {

    Optional<Medicine> createMedicine(Medicine medicine);

    boolean updateMedicine(Medicine medicine);

    boolean deleteMedicine(int id);

    boolean deactivateMedicine(int id);

    Optional<Medicine> getMedicineById(int id);

    List<Medicine> getAllMedicines(); 
    
    Optional<Medicine> getMedicineByBarcode(String barcode);

    List<Medicine> searchMedicines(String keyword);

    List<Medicine> getActiveMedicines();

    List<Medicine> getLowStockMedicines();

    List<Medicine> getMedicinesByCategory(int categoryId);
}