package com.pharmacy.models.purchasing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * نموذج بيانات يمثل فاتورة مشتريات (Purchase).
 * تم تحديثه ليرتبط بالوردية (shift_id) بدلاً من المستخدم لضمان سلامة التقارير المالية.
 */
public class Purchase {

    private int purchase_id;
    private int supplier_id;
    private int shift_id; // تم التعديل من user_id إلى shift_id
    private LocalDateTime purchase_date;
    private BigDecimal total_cost;
    private String payment_status;
    private String supplier_invoice_number; // الحقل الجديد
    private String supplier_name; // حقل للعرض فقط (Join Field)
    public Purchase() {
    }

    public Purchase(int supplier_id, int shift_id, LocalDateTime purchase_date, 
                    BigDecimal total_cost, String payment_status) {
        this.supplier_id = supplier_id;
        this.shift_id = shift_id;
        this.purchase_date = purchase_date;
        this.total_cost = total_cost;
        this.payment_status = payment_status;
    }

    public int getPurchase_id() { return purchase_id; }
    public void setPurchase_id(int purchase_id) { this.purchase_id = purchase_id; }

    public int getSupplier_id() { return supplier_id; }
    public void setSupplier_id(int supplier_id) { this.supplier_id = supplier_id; }

    // Getters & Setters المحدثة للوردية
    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public LocalDateTime getPurchase_date() { return purchase_date; }
    public void setPurchase_date(LocalDateTime purchase_date) { this.purchase_date = purchase_date; }

    public BigDecimal getTotal_cost() { return total_cost; }
    public void setTotal_cost(BigDecimal total_cost) { this.total_cost = total_cost; }

    public String getPayment_status() { return payment_status; }
    public void setPayment_status(String payment_status) { this.payment_status = payment_status; }
    
    public String getSupplier_invoice_number() { return supplier_invoice_number; }
    public void setSupplier_invoice_number(String supplier_invoice_number) { this.supplier_invoice_number = supplier_invoice_number; }

    public String getSupplier_name() { return supplier_name; }
    public void setSupplier_name(String supplier_name) { this.supplier_name = supplier_name; }
}