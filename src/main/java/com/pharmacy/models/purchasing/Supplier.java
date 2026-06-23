package com.pharmacy.models.purchasing;

public class Supplier {

    private int supplier_id;
    private String name;
    private String contact_person;
    private String phone;
    private String address;

    public Supplier() {
    }

    public Supplier(String name, String contact_person, String phone, String address) {
        this.name = name;
        this.contact_person = contact_person;
        this.phone = phone;
        this.address = address;
    }

    public int getSupplier_id() { return supplier_id; }
    public void setSupplier_id(int supplier_id) { this.supplier_id = supplier_id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact_person() { return contact_person; }
    public void setContact_person(String contact_person) { this.contact_person = contact_person; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}