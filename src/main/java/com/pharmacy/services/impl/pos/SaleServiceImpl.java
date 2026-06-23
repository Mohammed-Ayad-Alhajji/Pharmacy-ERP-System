// مسار الملف: src/main/java/com/pharmacy/services/impl/pos/SaleServiceImpl.java

package com.pharmacy.services.impl.pos;

import com.pharmacy.dao.interfaces.pos.SaleDAO;
import com.pharmacy.dao.interfaces.pos.SaleDetailDAO;
import com.pharmacy.models.pos.Sale;
import com.pharmacy.models.pos.SaleDetail;
import com.pharmacy.services.interfaces.pos.SaleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SaleServiceImpl implements SaleService {

    private final SaleDAO saleDAO;
    private final SaleDetailDAO saleDetailDAO;

    public SaleServiceImpl(SaleDAO saleDAO, SaleDetailDAO saleDetailDAO) {
        this.saleDAO = saleDAO;
        this.saleDetailDAO = saleDetailDAO;
    }

    @Override
    public Optional<Sale> createSale(Sale sale) {
        if (sale.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن ترتبط الفاتورة بوردية مفتوحة");
        }

        if (sale.getTotal_amount() == null || sale.getTotal_amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("إجمالي الفاتورة غير صالح");
        }

        if (sale.getPayment_method() == null || sale.getPayment_method().trim().isEmpty()) {
            throw new IllegalArgumentException("يجب تحديد طريقة الدفع");
        }

        if (sale.getSale_date() == null) {
            sale.setSale_date(LocalDateTime.now());
        }

        if (sale.getStatus() == null || sale.getStatus().trim().isEmpty()) {
            sale.setStatus("Completed");
        }

        return saleDAO.create(sale);
    }

    @Override
    public boolean updateSale(Sale sale) {
        if (sale.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن ترتبط الفاتورة بوردية مفتوحة");
        }

        if (sale.getTotal_amount() == null || sale.getTotal_amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("إجمالي الفاتورة غير صالح");
        }

        return saleDAO.update(sale);
    }

    @Override
    public Optional<Sale> getSaleById(int saleId) {
        return saleDAO.findById(saleId);
    }

    @Override
    public List<Sale> getSalesByShift(int shiftId) {
        return saleDAO.findByShiftId(shiftId);
    }

    @Override
    public List<Sale> getSalesByDateRange(LocalDate start, LocalDate end) {
        return saleDAO.findByDateRange(start, end);
    }

    @Override
    public List<Sale> getSalesByCustomer(int customerId) {
        return saleDAO.findByCustomerId(customerId);
    }

    @Override
    public List<Sale> getSalesByInsurance(int insuranceId) {
        return saleDAO.findByInsuranceId(insuranceId);
    }

    @Override
    public List<Sale> getSalesByStatus(String status) {
        return saleDAO.findByStatus(status);
    }

    @Override
    public List<Sale> getSalesByPaymentMethod(String method) {
        return saleDAO.findByPaymentMethod(method);
    }

    @Override
    public Optional<SaleDetail> addSaleDetail(SaleDetail detail) {
        if (detail.getSale_id() <= 0 || detail.getBatch_id() <= 0) {
            throw new IllegalArgumentException("بيانات الفاتورة أو التشغيلة مفقودة");
        }

        if (detail.getQuantity_sold() <= 0) {
            throw new IllegalArgumentException("الكمية المباعة يجب أن تكون أكبر من الصفر");
        }

        BigDecimal patientShare = detail.getPatient_share() != null ? detail.getPatient_share() : BigDecimal.ZERO;
        BigDecimal insuranceShare = detail.getInsurance_share() != null ? detail.getInsurance_share() : BigDecimal.ZERO;
        BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;

        if (subtotal.compareTo(patientShare.add(insuranceShare)) != 0) {
            throw new IllegalArgumentException("خطأ محاسبي: الإجمالي الفرعي لا يطابق مجموع حصة المريض والتأمين");
        }

        return saleDetailDAO.create(detail);
    }

    @Override
    public List<SaleDetail> getSaleDetailsBySaleId(int saleId) {
        return saleDetailDAO.findBySaleId(saleId);
    }

    @Override
    public List<SaleDetail> getSaleDetailsByBatch(int batchId) {
        return saleDetailDAO.findByBatchId(batchId);
    }
}