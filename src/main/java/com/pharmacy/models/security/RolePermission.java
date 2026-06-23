package com.pharmacy.models.security;

public class RolePermission {

    private int role_id;
    private int perm_id;

    public RolePermission() {
    }

    public RolePermission(int role_id, int perm_id) {
        this.role_id = role_id;
        this.perm_id = perm_id;
    }

    public int getRole_id() { return role_id; }
    public void setRole_id(int role_id) { this.role_id = role_id; }

    public int getPerm_id() { return perm_id; }
    public void setPerm_id(int perm_id) { this.perm_id = perm_id; }
}