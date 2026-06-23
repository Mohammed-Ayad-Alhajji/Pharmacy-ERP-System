package com.pharmacy.models.purchasing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplierPayment {

    private int payment_id;
    private Integer supplier_id;
    private Integer purchase_id;
    private int shift_id;
    private BigDecimal amount_paid;
    private String receipt_number;
    private LocalDateTime payment_date;
    private String payment_method;
    private String transaction_type; // الحقل الجديد (Payment أو Refund)

    public SupplierPayment() {
        this.transaction_type = "Payment"; // القيمة الافتراضية
    }

    public SupplierPayment(Integer supplier_id, Integer purchase_id, int shift_id, 
                           BigDecimal amount_paid, String receipt_number, 
                           LocalDateTime payment_date, String payment_method, String transaction_type) {
        this.supplier_id = supplier_id;
        this.purchase_id = purchase_id;
        this.shift_id = shift_id;
        this.amount_paid = amount_paid;
        this.receipt_number = receipt_number;
        this.payment_date = payment_date;
        this.payment_method = payment_method;
        this.transaction_type = (transaction_type != null) ? transaction_type : "Payment";
    }

    public int getPayment_id() { return payment_id; }
    public void setPayment_id(int payment_id) { this.payment_id = payment_id; }

    public Integer getSupplier_id() { return supplier_id; }
    public void setSupplier_id(Integer supplier_id) { this.supplier_id = supplier_id; }

    public Integer getPurchase_id() { return purchase_id; }
    public void setPurchase_id(Integer purchase_id) { this.purchase_id = purchase_id; }

    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public BigDecimal getAmount_paid() { return amount_paid; }
    public void setAmount_paid(BigDecimal amount_paid) { this.amount_paid = amount_paid; }

    public String getReceipt_number() { return receipt_number; }
    public void setReceipt_number(String receipt_number) { this.receipt_number = receipt_number; }

    public LocalDateTime getPayment_date() { return payment_date; }
    public void setPayment_date(LocalDateTime payment_date) { this.payment_date = payment_date; }

    public String getPayment_method() { return payment_method; }
    public void setPayment_method(String payment_method) { this.payment_method = payment_method; }

    public String getTransaction_type() { return transaction_type; }
    public void setTransaction_type(String transaction_type) { this.transaction_type = transaction_type; }
}