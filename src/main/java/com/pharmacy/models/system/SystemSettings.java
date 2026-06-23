package com.pharmacy.models.system;

public class SystemSettings {

    private int setting_id;
    private String pharmacy_name;
    private String logo_path;
    private String address;
    private String phone;
    private String currency_symbol;

    public SystemSettings() {
    }

    public SystemSettings(String pharmacy_name, String logo_path, String address, String phone, String currency_symbol) {
        this.pharmacy_name = pharmacy_name;
        this.logo_path = logo_path;
        this.address = address;
        this.phone = phone;
        this.currency_symbol = currency_symbol;
    }

    public int getSetting_id() { return setting_id; }
    public void setSetting_id(int setting_id) { this.setting_id = setting_id; }

    public String getPharmacy_name() { return pharmacy_name; }
    public void setPharmacy_name(String pharmacy_name) { this.pharmacy_name = pharmacy_name; }

    public String getLogo_path() { return logo_path; }
    public void setLogo_path(String logo_path) { this.logo_path = logo_path; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCurrency_symbol() { return currency_symbol; }
    public void setCurrency_symbol(String currency_symbol) { this.currency_symbol = currency_symbol; }
}