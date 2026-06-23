package com.pharmacy.models.system;

import java.time.LocalDateTime;

public class AuditLog {

    private int log_id;
    private Integer user_id;
    private String action_type;
    private String table_affected;
    private String old_data;
    private String new_data;
    private LocalDateTime action_timestamp;

    public AuditLog() {
    }

    public AuditLog(Integer user_id, String action_type, String table_affected, 
                    String old_data, String new_data, LocalDateTime action_timestamp) {
        this.user_id = user_id;
        this.action_type = action_type;
        this.table_affected = table_affected;
        this.old_data = old_data;
        this.new_data = new_data;
        this.action_timestamp = action_timestamp;
    }

    public int getLog_id() { return log_id; }
    public void setLog_id(int log_id) { this.log_id = log_id; }

    public Integer getUser_id() { return user_id; }
    public void setUser_id(Integer user_id) { this.user_id = user_id; }

    public String getAction_type() { return action_type; }
    public void setAction_type(String action_type) { this.action_type = action_type; }

    public String getTable_affected() { return table_affected; }
    public void setTable_affected(String table_affected) { this.table_affected = table_affected; }

    public String getOld_data() { return old_data; }
    public void setOld_data(String old_data) { this.old_data = old_data; }

    public String getNew_data() { return new_data; }
    public void setNew_data(String new_data) { this.new_data = new_data; }

    public LocalDateTime getAction_timestamp() { return action_timestamp; }
    public void setAction_timestamp(LocalDateTime action_timestamp) { this.action_timestamp = action_timestamp; }

}