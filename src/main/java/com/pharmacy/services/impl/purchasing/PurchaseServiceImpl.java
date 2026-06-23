// مسار الملف: src/main/java/com/pharmacy/services/impl/purchasing/PurchaseServiceImpl.java

package com.pharmacy.services.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.PurchaseDAO;
import com.pharmacy.dao.interfaces.purchasing.PurchaseDetailDAO;
import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.purchasing.PurchaseDetail;
import com.pharmacy.services.interfaces.purchasing.PurchaseService;
import com.pharmacy.security.SessionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseDAO purchaseDAO;
    private final PurchaseDetailDAO purchaseDetailDAO;

    public PurchaseServiceImpl(PurchaseDAO purchaseDAO, PurchaseDetailDAO purchaseDetailDAO) {
        this.purchaseDAO = purchaseDAO;
        this.purchaseDetailDAO = purchaseDetailDAO;
    }

    @Override
    public Optional<Purchase> createPurchase(Purchase purchase) {
        if (purchase.getSupplier_id() <= 0) {
            throw new IllegalArgumentException("يجب اختيار المورد");
        }

        if (purchase.getTotal_cost() == null || purchase.getTotal_cost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("إجمالي الفاتورة غير صالح");
        }

        try {
            // التعديل الهندسي الصحيح: جلب الوردية المفتوحة بدلاً من المستخدم
            com.pharmacy.models.security.Shift currentShift = com.pharmacy.security.SessionManager.getInstance().getCurrentShift();
            
            // جدار أمني: التأكد من وجود وردية مفتوحة بالفعل
            if (currentShift == null) {
                throw new IllegalStateException("لا يوجد وردية مفتوحة حالياً. يرجى فتح وردية قبل الشراء.");
            }
            
            // ربط الفاتورة برقم الوردية
            purchase.setShift_id(currentShift.getShift_id());
            
        } catch (Exception e) {
            throw new IllegalStateException("حدث خطأ في الجلسة: " + e.getMessage());
        }

        if (purchase.getPurchase_date() == null) {
            purchase.setPurchase_date(LocalDateTime.now());
        }

        if (purchase.getPayment_status() == null || purchase.getPayment_status().trim().isEmpty()) {
            purchase.setPayment_status("Unpaid");
        }

        return purchaseDAO.create(purchase);
    }

    @Override
    public boolean updatePurchase(Purchase purchase) {
        if (purchase.getSupplier_id() <= 0) {
            throw new IllegalArgumentException("يجب اختيار المورد");
        }

        if (purchase.getTotal_cost() == null || purchase.getTotal_cost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("إجمالي الفاتورة غير صالح");
        }

        return purchaseDAO.update(purchase);
    }

    @Override
    public boolean deletePurchase(int purchaseId) {
        try {
            boolean isDeleted = purchaseDAO.delete(purchaseId);
            if (!isDeleted) {
                throw new IllegalStateException("لا يمكن حذف الفاتورة لارتباطها بدفعات أو مرتجعات في النظام.");
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("لا يمكن حذف الفاتورة لارتباطها بدفعات أو مرتجعات في النظام.", e);
        }
    }

    @Override
    public Optional<Purchase> getPurchaseById(int purchaseId) {
        return purchaseDAO.findById(purchaseId);
    }

    @Override
    public List<Purchase> getPurchasesBySupplier(int supplierId) {
        return purchaseDAO.findBySupplierId(supplierId);
    }

    @Override
    public List<Purchase> getPurchasesByDateRange(LocalDate start, LocalDate end) {
        return purchaseDAO.findByDateRange(start, end);
    }

    @Override
    public Optional<PurchaseDetail> addPurchaseDetail(PurchaseDetail detail) {
        if (detail.getPurchase_id() <= 0 || detail.getBatch_id() <= 0) {
            throw new IllegalArgumentException("بيانات الفاتورة أو التشغيلة مفقودة");
        }

        if (detail.getQuantity_received() <= 0) {
            throw new IllegalArgumentException("الكمية المستلمة يجب أن تكون أكبر من الصفر");
        }

        if (detail.getBonus_quantity() < 0) {
            throw new IllegalArgumentException("كمية البونص لا يمكن أن تكون سالبة");
        }

        if (detail.getBox_cost() == null || detail.getBox_cost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("سعر التكلفة غير صالح");
        }

        return purchaseDetailDAO.create(detail);
    }

    @Override
    public boolean deletePurchaseDetail(int pdId) {
        return purchaseDetailDAO.delete(pdId);
    }

    @Override
    public List<PurchaseDetail> getPurchaseDetailsByPurchaseId(int purchaseId) {
        return purchaseDetailDAO.findByPurchaseId(purchaseId);
    }
    
    @Override
    public List<Purchase> findAll() {
        return purchaseDAO.findAll();
    }
    
    @Override
    public boolean updatePaymentStatus(int purchaseId, String status) {
        if (purchaseId <= 0 || status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("بيانات تحديث الحالة غير صالحة");
        }
        return purchaseDAO.updatePaymentStatus(purchaseId, status);
    }
}