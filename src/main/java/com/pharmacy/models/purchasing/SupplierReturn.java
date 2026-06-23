package com.pharmacy.models.purchasing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplierReturn {

    private int sup_return_id;
    private int purchase_id;
    private int batch_id;
    private int user_id;
    private int quantity_returned;
    private BigDecimal total_refund_value;
    private String return_status;
    private LocalDateTime return_date;
    private String reason;

    public SupplierReturn() {
    }

    public SupplierReturn(int purchase_id, int batch_id, int user_id, int quantity_returned, 
                          BigDecimal total_refund_value, String return_status, 
                          LocalDateTime return_date, String reason) {
        this.purchase_id = purchase_id;
        this.batch_id = batch_id;
        this.user_id = user_id;
        this.quantity_returned = quantity_returned;
        this.total_refund_value = total_refund_value;
        this.return_status = return_status;
        this.return_date = return_date;
        this.reason = reason;
    }

    public int getSup_return_id() { return sup_return_id; }
    public void setSup_return_id(int sup_return_id) { this.sup_return_id = sup_return_id; }

    public int getPurchase_id() { return purchase_id; }
    public void setPurchase_id(int purchase_id) { this.purchase_id = purchase_id; }

    public int getBatch_id() { return batch_id; }
    public void setBatch_id(int batch_id) { this.batch_id = batch_id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getQuantity_returned() { return quantity_returned; }
    public void setQuantity_returned(int quantity_returned) { this.quantity_returned = quantity_returned; }

    public BigDecimal getTotal_refund_value() { return total_refund_value; }
    public void setTotal_refund_value(BigDecimal total_refund_value) { this.total_refund_value = total_refund_value; }

    public String getReturn_status() { return return_status; }
    public void setReturn_status(String return_status) { this.return_status = return_status; }

    public LocalDateTime getReturn_date() { return return_date; }
    public void setReturn_date(LocalDateTime return_date) { this.return_date = return_date; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    
}