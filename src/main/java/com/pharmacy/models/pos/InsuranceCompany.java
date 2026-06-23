package com.pharmacy.models.pos;

public class InsuranceCompany {

    private int insurance_id;
    private String name;
    private String contact_info;
    private String address;

    public InsuranceCompany() {
    }

    public InsuranceCompany(String name, String contact_info, String address) {
        this.name = name;
        this.contact_info = contact_info;
        this.address = address;
    }

    public int getInsurance_id() { return insurance_id; }
    public void setInsurance_id(int insurance_id) { this.insurance_id = insurance_id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact_info() { return contact_info; }
    public void setContact_info(String contact_info) { this.contact_info = contact_info; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}