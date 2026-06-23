package com.pharmacy.models.security;

public class User {

    private int user_id;
    private int role_id;
    private String username;
    private String password_hash;
    private String full_name;
    private boolean isActive; // تم التعديل إلى boolean

    public User() {
    }

    public User(int role_id, String username, String password_hash, String full_name, boolean isActive) {
        this.role_id = role_id;
        this.username = username;
        this.password_hash = password_hash;
        this.full_name = full_name;
        this.isActive = isActive;
    }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getRole_id() { return role_id; }
    public void setRole_id(int role_id) { this.role_id = role_id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword_hash() { return password_hash; }
    public void setPassword_hash(String password_hash) { this.password_hash = password_hash; }

    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }

    // معايير الجافا للـ boolean تستخدم is بدلاً من get
    public boolean isActive() { return isActive; } 
    public void setActive(boolean isActive) { this.isActive = isActive; }
}