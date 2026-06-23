// مسار الملف: src/main/java/com/pharmacy/services/interfaces/pos/SaleService.java

package com.pharmacy.services.interfaces.pos;

import com.pharmacy.models.pos.Sale;
import com.pharmacy.models.pos.SaleDetail;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleService {

    Optional<Sale> createSale(Sale sale);

    boolean updateSale(Sale sale);

    Optional<Sale> getSaleById(int saleId);

    List<Sale> getSalesByShift(int shiftId);

    List<Sale> getSalesByDateRange(LocalDate start, LocalDate end);

    List<Sale> getSalesByCustomer(int customerId);

    List<Sale> getSalesByInsurance(int insuranceId);

    List<Sale> getSalesByStatus(String status);

    List<Sale> getSalesByPaymentMethod(String method);

    Optional<SaleDetail> addSaleDetail(SaleDetail detail);

    List<SaleDetail> getSaleDetailsBySaleId(int saleId);

    List<SaleDetail> getSaleDetailsByBatch(int batchId);
}