// مسار الملف: src/main/java/com/pharmacy/services/interfaces/inventory/BatchService.java

package com.pharmacy.services.interfaces.inventory;

import com.pharmacy.models.inventory.Batch;

import java.util.List;
import java.util.Optional;

public interface BatchService {

    Optional<Batch> createBatch(Batch batch);

    boolean updateBatch(Batch batch);

    boolean deactivateBatch(int batchId);

    Optional<Batch> getBatchById(int batchId);

    List<Batch> getBatchesByMedicine(int medId);

    List<Batch> getActiveBatchesByMedicine(int medId);

    List<Batch> getNearExpiryBatches(int daysThreshold);

    Optional<Batch> getBatchByNumberAndMedicine(String batchNumber, int medId);

    List<Batch> getValidBatchesForSale(int medId);
    
    Optional<Batch> getLastBatchByMedicineId(int medId);
    
    List<Batch> getAllBatches(); // أضفها في واجهة BatchService
}