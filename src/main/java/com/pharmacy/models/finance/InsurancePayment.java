package com.pharmacy.models.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InsurancePayment {

    private int payment_id;
    private Integer insurance_id;
    private Integer sale_id;
    private int shift_id;
    private BigDecimal amount_paid;
    private String payment_method;
    private String reference_number;
    private LocalDateTime payment_date;

    public InsurancePayment() {
    }

    public InsurancePayment(Integer insurance_id, Integer sale_id, int shift_id, 
                            BigDecimal amount_paid, String payment_method, 
                            String reference_number, LocalDateTime payment_date) {
        this.insurance_id = insurance_id;
        this.sale_id = sale_id;
        this.shift_id = shift_id;
        this.amount_paid = amount_paid;
        this.payment_method = payment_method;
        this.reference_number = reference_number;
        this.payment_date = payment_date;
    }

    public int getPayment_id() { return payment_id; }
    public void setPayment_id(int payment_id) { this.payment_id = payment_id; }

    public Integer getInsurance_id() { return insurance_id; }
    public void setInsurance_id(Integer insurance_id) { this.insurance_id = insurance_id; }

    public Integer getSale_id() { return sale_id; }
    public void setSale_id(Integer sale_id) { this.sale_id = sale_id; }

    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public BigDecimal getAmount_paid() { return amount_paid; }
    public void setAmount_paid(BigDecimal amount_paid) { this.amount_paid = amount_paid; }

    public String getPayment_method() { return payment_method; }
    public void setPayment_method(String payment_method) { this.payment_method = payment_method; }

    public String getReference_number() { return reference_number; }
    public void setReference_number(String reference_number) { this.reference_number = reference_number; }

    public LocalDateTime getPayment_date() { return payment_date; }
    public void setPayment_date(LocalDateTime payment_date) { this.payment_date = payment_date; }
}