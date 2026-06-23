package com.pharmacy.models.pos;

import java.math.BigDecimal;

public class SaleDetail {

    private int detail_id;
    private int sale_id;
    private int batch_id;
    private int quantity_sold;
    private BigDecimal unit_sell_price;
    private BigDecimal patient_share;
    private BigDecimal insurance_share;
    private BigDecimal subtotal;

    public SaleDetail() {
    }

    public SaleDetail(int sale_id, int batch_id, int quantity_sold, BigDecimal unit_sell_price, 
                      BigDecimal patient_share, BigDecimal insurance_share, BigDecimal subtotal) {
        this.sale_id = sale_id;
        this.batch_id = batch_id;
        this.quantity_sold = quantity_sold;
        this.unit_sell_price = unit_sell_price;
        this.patient_share = patient_share;
        this.insurance_share = insurance_share;
        this.subtotal = subtotal;
    }

    public int getDetail_id() { return detail_id; }
    public void setDetail_id(int detail_id) { this.detail_id = detail_id; }

    public int getSale_id() { return sale_id; }
    public void setSale_id(int sale_id) { this.sale_id = sale_id; }

    public int getBatch_id() { return batch_id; }
    public void setBatch_id(int batch_id) { this.batch_id = batch_id; }

    public int getQuantity_sold() { return quantity_sold; }
    public void setQuantity_sold(int quantity_sold) { this.quantity_sold = quantity_sold; }

    public BigDecimal getUnit_sell_price() { return unit_sell_price; }
    public void setUnit_sell_price(BigDecimal unit_sell_price) { this.unit_sell_price = unit_sell_price; }

    public BigDecimal getPatient_share() { return patient_share; }
    public void setPatient_share(BigDecimal patient_share) { this.patient_share = patient_share; }

    public BigDecimal getInsurance_share() { return insurance_share; }
    public void setInsurance_share(BigDecimal insurance_share) { this.insurance_share = insurance_share; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}