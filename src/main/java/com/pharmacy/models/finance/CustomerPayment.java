package com.pharmacy.models.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CustomerPayment {

    private int payment_id;
    private Integer customer_id;
    private Integer sale_id;
    private int shift_id;
    private BigDecimal amount_paid;
    private LocalDateTime payment_date;

    public CustomerPayment() {
    }

    public CustomerPayment(Integer customer_id, Integer sale_id, int shift_id, 
                           BigDecimal amount_paid, LocalDateTime payment_date) {
        this.customer_id = customer_id;
        this.sale_id = sale_id;
        this.shift_id = shift_id;
        this.amount_paid = amount_paid;
        this.payment_date = payment_date;
    }

    public int getPayment_id() { return payment_id; }
    public void setPayment_id(int payment_id) { this.payment_id = payment_id; }

    public Integer getCustomer_id() { return customer_id; }
    public void setCustomer_id(Integer customer_id) { this.customer_id = customer_id; }

    public Integer getSale_id() { return sale_id; }
    public void setSale_id(Integer sale_id) { this.sale_id = sale_id; }

    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public BigDecimal getAmount_paid() { return amount_paid; }
    public void setAmount_paid(BigDecimal amount_paid) { this.amount_paid = amount_paid; }

    public LocalDateTime getPayment_date() { return payment_date; }
    public void setPayment_date(LocalDateTime payment_date) { this.payment_date = payment_date; }
}