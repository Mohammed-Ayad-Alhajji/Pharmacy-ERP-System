// src/main/java/com/pharmacy/dao/interfaces/inventory/DrugAlternativeDAO.java
package com.pharmacy.dao.interfaces.inventory;

import com.pharmacy.models.inventory.Medicine;
import java.util.List;

/**
 * Data Access Object interface for Drug_Alternatives junction table.
 * Distinct from GenericDAO as it handles a composite primary key relationship without a dedicated single-ID entity.
 */
public interface DrugAlternativeDAO {
    
    boolean addAlternative(int medId1, int medId2);
    
    boolean removeAlternative(int medId1, int medId2);
    // جلب قائمة الأدوية البديلة لدواء معين
    List<Medicine> getAlternatives(int medId);
    boolean exists(int medId1, int medId2);
}