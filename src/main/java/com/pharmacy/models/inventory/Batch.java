package com.pharmacy.models.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Batch {

    private int batch_id;
    private int med_id;
    private String batch_number;
    private LocalDate mfg_date;
    private LocalDate exp_date;
    private int quantity;
    private BigDecimal buy_box_cost;
    private int is_active;

    public Batch() {
    }

    public Batch(int med_id, String batch_number, LocalDate mfg_date, LocalDate exp_date, 
                 int quantity, BigDecimal buy_box_cost, int is_active) {
        this.med_id = med_id;
        this.batch_number = batch_number;
        this.mfg_date = mfg_date;
        this.exp_date = exp_date;
        this.quantity = quantity;
        this.buy_box_cost = buy_box_cost;
        this.is_active = is_active;
    }

    public int getBatch_id() { return batch_id; }
    public void setBatch_id(int batch_id) { this.batch_id = batch_id; }

    public int getMed_id() { return med_id; }
    public void setMed_id(int med_id) { this.med_id = med_id; }

    public String getBatch_number() { return batch_number; }
    public void setBatch_number(String batch_number) { this.batch_number = batch_number; }

    public LocalDate getMfg_date() { return mfg_date; }
    public void setMfg_date(LocalDate mfg_date) { this.mfg_date = mfg_date; }

    public LocalDate getExp_date() { return exp_date; }
    public void setExp_date(LocalDate exp_date) { this.exp_date = exp_date; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getBuy_box_cost() { return buy_box_cost; }
    public void setBuy_box_cost(BigDecimal buy_box_cost) { this.buy_box_cost = buy_box_cost; }

    public int getIs_active() { return is_active; }
    public void setIs_active(int is_active) { this.is_active = is_active; }
    
    // هذه الدالة تخبر JavaFX و ControlsFX كيف تعرض هذا الكائن كنص في القوائم
    @Override
    public String toString() {
        return this.batch_number != null ? this.batch_number : "";
    }
}