package com.pharmacy.models.purchasing;

import java.math.BigDecimal;

public class PurchaseDetail {

    private int pd_id;
    private int purchase_id;
    private int batch_id;
    private int quantity_received;
    private int bonus_quantity;
    private BigDecimal box_cost;

    public PurchaseDetail() {
    }

    public PurchaseDetail(int purchase_id, int batch_id, int quantity_received, 
                          int bonus_quantity, BigDecimal box_cost) {
        this.purchase_id = purchase_id;
        this.batch_id = batch_id;
        this.quantity_received = quantity_received;
        this.bonus_quantity = bonus_quantity;
        this.box_cost = box_cost;
    }

    public int getPd_id() { return pd_id; }
    public void setPd_id(int pd_id) { this.pd_id = pd_id; }

    public int getPurchase_id() { return purchase_id; }
    public void setPurchase_id(int purchase_id) { this.purchase_id = purchase_id; }

    public int getBatch_id() { return batch_id; }
    public void setBatch_id(int batch_id) { this.batch_id = batch_id; }

    public int getQuantity_received() { return quantity_received; }
    public void setQuantity_received(int quantity_received) { this.quantity_received = quantity_received; }

    public int getBonus_quantity() { return bonus_quantity; }
    public void setBonus_quantity(int bonus_quantity) { this.bonus_quantity = bonus_quantity; }

    public BigDecimal getBox_cost() { return box_cost; }
    public void setBox_cost(BigDecimal box_cost) { this.box_cost = box_cost; }
}