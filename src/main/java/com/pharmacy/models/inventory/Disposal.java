package com.pharmacy.models.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Disposal {

    private int disposal_id;
    private int batch_id;
    private int user_id;
    private int quantity_disposed;
    private BigDecimal total_cost;
    private BigDecimal pharmacy_loss_amount;
    private BigDecimal supplier_compensation_amount;
    private LocalDateTime disposal_date;
    private String reason;

    public Disposal() {
    }

    public Disposal(int batch_id, int user_id, int quantity_disposed, BigDecimal total_cost, 
                    BigDecimal pharmacy_loss_amount, BigDecimal supplier_compensation_amount, 
                    LocalDateTime disposal_date, String reason) {
        this.batch_id = batch_id;
        this.user_id = user_id;
        this.quantity_disposed = quantity_disposed;
        this.total_cost = total_cost;
        this.pharmacy_loss_amount = pharmacy_loss_amount;
        this.supplier_compensation_amount = supplier_compensation_amount;
        this.disposal_date = disposal_date;
        this.reason = reason;
    }

    public int getDisposal_id() { return disposal_id; }
    public void setDisposal_id(int disposal_id) { this.disposal_id = disposal_id; }

    public int getBatch_id() { return batch_id; }
    public void setBatch_id(int batch_id) { this.batch_id = batch_id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getQuantity_disposed() { return quantity_disposed; }
    public void setQuantity_disposed(int quantity_disposed) { this.quantity_disposed = quantity_disposed; }

    public BigDecimal getTotal_cost() { return total_cost; }
    public void setTotal_cost(BigDecimal total_cost) { this.total_cost = total_cost; }

    public BigDecimal getPharmacy_loss_amount() { return pharmacy_loss_amount; }
    public void setPharmacy_loss_amount(BigDecimal pharmacy_loss_amount) { this.pharmacy_loss_amount = pharmacy_loss_amount; }

    public BigDecimal getSupplier_compensation_amount() { return supplier_compensation_amount; }
    public void setSupplier_compensation_amount(BigDecimal supplier_compensation_amount) { this.supplier_compensation_amount = supplier_compensation_amount; }

    public LocalDateTime getDisposal_date() { return disposal_date; }
    public void setDisposal_date(LocalDateTime disposal_date) { this.disposal_date = disposal_date; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}