package com.pharmacy.models.inventory;

import java.time.LocalDateTime;

public class InventoryAdjustment {

    private int adjustment_id;
    private int batch_id;
    private int user_id;
    private int system_quantity;
    private int actual_quantity;
    private int difference;
    private LocalDateTime adjustment_date;
    private String notes;

    public InventoryAdjustment() {
    }

    public InventoryAdjustment(int batch_id, int user_id, int system_quantity, 
                               int actual_quantity, int difference, 
                               LocalDateTime adjustment_date, String notes) {
        this.batch_id = batch_id;
        this.user_id = user_id;
        this.system_quantity = system_quantity;
        this.actual_quantity = actual_quantity;
        this.difference = difference;
        this.adjustment_date = adjustment_date;
        this.notes = notes;
    }

    public int getAdjustment_id() { return adjustment_id; }
    public void setAdjustment_id(int adjustment_id) { this.adjustment_id = adjustment_id; }

    public int getBatch_id() { return batch_id; }
    public void setBatch_id(int batch_id) { this.batch_id = batch_id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getSystem_quantity() { return system_quantity; }
    public void setSystem_quantity(int system_quantity) { this.system_quantity = system_quantity; }

    public int getActual_quantity() { return actual_quantity; }
    public void setActual_quantity(int actual_quantity) { this.actual_quantity = actual_quantity; }

    public int getDifference() { return difference; }
    public void setDifference(int difference) { this.difference = difference; }

    public LocalDateTime getAdjustment_date() { return adjustment_date; }
    public void setAdjustment_date(LocalDateTime adjustment_date) { this.adjustment_date = adjustment_date; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}