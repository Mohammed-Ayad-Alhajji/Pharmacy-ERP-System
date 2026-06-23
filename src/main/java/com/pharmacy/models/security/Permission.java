package com.pharmacy.models.security;

public class Permission {

    private int perm_id;
    private String perm_name;
    private String module;

    public Permission() {
    }

    public Permission(String perm_name, String module) {
        this.perm_name = perm_name;
        this.module = module;
    }

    public int getPerm_id() { return perm_id; }
    public void setPerm_id(int perm_id) { this.perm_id = perm_id; }

    public String getPerm_name() { return perm_name; }
    public void setPerm_name(String perm_name) { this.perm_name = perm_name; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
}