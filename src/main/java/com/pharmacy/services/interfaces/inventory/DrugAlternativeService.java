// مسار الملف: src/main/java/com/pharmacy/services/interfaces/inventory/DrugAlternativeService.java

package com.pharmacy.services.interfaces.inventory;

import com.pharmacy.models.inventory.Medicine;
import java.util.List;

/**
 * واجهة خدمة إدارة البدائل الدوائية.
 * تتبع مبدأ فصل المسؤوليات وتوفر العمليات الأساسية لربط الأدوية ببدائلها.
 */
public interface DrugAlternativeService {

    boolean addAlternative(int medId1, int medId2);

    boolean removeAlternative(int medId1, int medId2);

    boolean checkAlternativeExists(int medId1, int medId2);
    
    // جلب قائمة الأدوية البديلة لدواء معين
    List<Medicine> getAlternatives(int medId);
}