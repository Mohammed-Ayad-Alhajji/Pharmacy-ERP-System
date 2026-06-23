// مسار الملف: src/main/java/com/pharmacy/services/interfaces/purchasing/PurchaseService.java

package com.pharmacy.services.interfaces.purchasing;

import com.pharmacy.models.purchasing.Purchase;
import com.pharmacy.models.purchasing.PurchaseDetail;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseService {

    Optional<Purchase> createPurchase(Purchase purchase);

    boolean updatePurchase(Purchase purchase);

    boolean deletePurchase(int purchaseId);

    Optional<Purchase> getPurchaseById(int purchaseId);

    List<Purchase> getPurchasesBySupplier(int supplierId);

    List<Purchase> getPurchasesByDateRange(LocalDate start, LocalDate end);

    Optional<PurchaseDetail> addPurchaseDetail(PurchaseDetail detail);

    boolean deletePurchaseDetail(int pdId);

    List<PurchaseDetail> getPurchaseDetailsByPurchaseId(int purchaseId);
    List<Purchase> findAll();
    
    boolean updatePaymentStatus(int purchaseId, String status);
}