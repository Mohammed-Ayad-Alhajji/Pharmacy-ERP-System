// مسار الملف: src/main/java/com/pharmacy/services/impl/inventory/BatchServiceImpl.java

package com.pharmacy.services.impl.inventory;

import com.pharmacy.dao.interfaces.inventory.BatchDAO;
import com.pharmacy.models.inventory.Batch;
import com.pharmacy.services.interfaces.inventory.BatchService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class BatchServiceImpl implements BatchService {

    private final BatchDAO batchDAO;

    public BatchServiceImpl(BatchDAO batchDAO) {
        this.batchDAO = batchDAO;
    }

    private void validateBatch(Batch batch) {
        if (batch.getMed_id() <= 0) {
            throw new IllegalArgumentException("رقم الدواء غير صالح");
        }
        
        if (batch.getBatch_number() == null || batch.getBatch_number().trim().isEmpty()) {
            throw new IllegalArgumentException("رقم التشغيلة مطلوب");
        }
        
        if (batch.getQuantity() < 0) {
            throw new IllegalArgumentException("الكمية لا يمكن أن تكون سالبة");
        }
        
        if (batch.getBuy_box_cost() == null || batch.getBuy_box_cost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("سعر التكلفة غير صالح");
        }
        
        if (batch.getExp_date() == null) {
            throw new IllegalArgumentException("تاريخ الصلاحية مطلوب");
        }
        
        if (batch.getMfg_date() != null) {
            if (!batch.getExp_date().isAfter(batch.getMfg_date())) {
                throw new IllegalArgumentException("تاريخ الصلاحية يجب أن يكون بعد تاريخ الإنتاج");
            }
        }
    }

    @Override
    public Optional<Batch> createBatch(Batch batch) {
        validateBatch(batch);

        Optional<Batch> existing = batchDAO.findByBatchNumberAndMedicineId(batch.getBatch_number(), batch.getMed_id());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("رقم التشغيلة هذا موجود مسبقاً لهذا الدواء");
        }

        return batchDAO.create(batch);
    }

    @Override
    public boolean updateBatch(Batch batch) {
        validateBatch(batch);

        Optional<Batch> existing = batchDAO.findByBatchNumberAndMedicineId(batch.getBatch_number(), batch.getMed_id());
        if (existing.isPresent() && existing.get().getBatch_id() != batch.getBatch_id()) {
            throw new IllegalArgumentException("رقم التشغيلة موجود لتشغيلة أخرى");
        }

        return batchDAO.update(batch);
    }

    @Override
    public boolean deactivateBatch(int batchId) {
        Optional<Batch> optBatch = batchDAO.findById(batchId);
        if (optBatch.isPresent()) {
            Batch batch = optBatch.get();
            batch.setIs_active(0);
            return batchDAO.update(batch);
        }
        return false;
    }

    @Override
    public Optional<Batch> getBatchById(int batchId) {
        return batchDAO.findById(batchId);
    }
    
    @Override
    public Optional<Batch> getLastBatchByMedicineId(int medId) {
        return batchDAO.getLastBatchByMedicineId(medId);
    }
    @Override
    public List<Batch> getBatchesByMedicine(int medId) {
        return batchDAO.findByMedicineId(medId);
    }

    @Override
    public List<Batch> getActiveBatchesByMedicine(int medId) {
        return batchDAO.findActiveBatchesByMedicine(medId);
    }

    @Override
    public List<Batch> getNearExpiryBatches(int daysThreshold) {
        return batchDAO.findNearExpiryBatches(daysThreshold);
    }

    @Override
    public Optional<Batch> getBatchByNumberAndMedicine(String batchNumber, int medId) {
        return batchDAO.findByBatchNumberAndMedicineId(batchNumber, medId);
    }

    @Override
    public List<Batch> getValidBatchesForSale(int medId) {
        return batchDAO.findValidBatchesOrderedByExpiry(medId);
    }
    
    @Override
    public List<Batch> getAllBatches() {
        return batchDAO.findAll(); // أضفها في كلاس BatchServiceImpl
    }
}