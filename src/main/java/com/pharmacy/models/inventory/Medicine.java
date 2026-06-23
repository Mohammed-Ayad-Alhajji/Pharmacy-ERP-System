package com.pharmacy.models.inventory;

import java.math.BigDecimal;

public class Medicine {

    private int med_id;
    private String barcode;
    private String brand_name;
    private String generic_name;
    private int category_id;
    private String dosage_form;
    private int conversion_factor;
    private BigDecimal current_box_sell_price;
    private BigDecimal current_unit_sell_price;
    private int prescription_required;
    private int min_stock_level;
    private String shelf_location;
    private int is_active;

    public Medicine() {
    }

    public Medicine(String barcode, String brand_name, String generic_name, int category_id, 
                    String dosage_form, int conversion_factor, BigDecimal current_box_sell_price, 
                    BigDecimal current_unit_sell_price, int prescription_required, 
                    int min_stock_level, String shelf_location, int is_active) {
        this.barcode = barcode;
        this.brand_name = brand_name;
        this.generic_name = generic_name;
        this.category_id = category_id;
        this.dosage_form = dosage_form;
        this.conversion_factor = conversion_factor;
        this.current_box_sell_price = current_box_sell_price;
        this.current_unit_sell_price = current_unit_sell_price;
        this.prescription_required = prescription_required;
        this.min_stock_level = min_stock_level;
        this.shelf_location = shelf_location;
        this.is_active = is_active;
    }

    public int getMed_id() { return med_id; }
    public void setMed_id(int med_id) { this.med_id = med_id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getBrand_name() { return brand_name; }
    public void setBrand_name(String brand_name) { this.brand_name = brand_name; }

    public String getGeneric_name() { return generic_name; }
    public void setGeneric_name(String generic_name) { this.generic_name = generic_name; }

    public int getCategory_id() { return category_id; }
    public void setCategory_id(int category_id) { this.category_id = category_id; }

    public String getDosage_form() { return dosage_form; }
    public void setDosage_form(String dosage_form) { this.dosage_form = dosage_form; }

    public int getConversion_factor() { return conversion_factor; }
    public void setConversion_factor(int conversion_factor) { this.conversion_factor = conversion_factor; }

    public BigDecimal getCurrent_box_sell_price() { return current_box_sell_price; }
    public void setCurrent_box_sell_price(BigDecimal current_box_sell_price) { this.current_box_sell_price = current_box_sell_price; }

    public BigDecimal getCurrent_unit_sell_price() { return current_unit_sell_price; }
    public void setCurrent_unit_sell_price(BigDecimal current_unit_sell_price) { this.current_unit_sell_price = current_unit_sell_price; }

    public int getPrescription_required() { return prescription_required; }
    public void setPrescription_required(int prescription_required) { this.prescription_required = prescription_required; }

    public int getMin_stock_level() { return min_stock_level; }
    public void setMin_stock_level(int min_stock_level) { this.min_stock_level = min_stock_level; }

    public String getShelf_location() { return shelf_location; }
    public void setShelf_location(String shelf_location) { this.shelf_location = shelf_location; }

    public int getIs_active() { return is_active; }
    public void setIs_active(int is_active) { this.is_active = is_active; }
}