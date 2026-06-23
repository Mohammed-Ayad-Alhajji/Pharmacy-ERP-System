package com.pharmacy.models.security;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Shift {

    private int shift_id;
    private int user_id;
    private LocalDateTime start_time;
    private LocalDateTime end_time;
    private BigDecimal opening_balance;
    private BigDecimal expected_closing_balance;
    private BigDecimal actual_closing_balance;
    private String status;

    public Shift() {
    }

    public Shift(int user_id, LocalDateTime start_time, LocalDateTime end_time, BigDecimal opening_balance, 
                 BigDecimal expected_closing_balance, BigDecimal actual_closing_balance, String status) {
        this.user_id = user_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.opening_balance = opening_balance;
        this.expected_closing_balance = expected_closing_balance;
        this.actual_closing_balance = actual_closing_balance;
        this.status = status;
    }

    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public LocalDateTime getStart_time() { return start_time; }
    public void setStart_time(LocalDateTime start_time) { this.start_time = start_time; }

    public LocalDateTime getEnd_time() { return end_time; }
    public void setEnd_time(LocalDateTime end_time) { this.end_time = end_time; }

    public BigDecimal getOpening_balance() { return opening_balance; }
    public void setOpening_balance(BigDecimal opening_balance) { this.opening_balance = opening_balance; }

    public BigDecimal getExpected_closing_balance() { return expected_closing_balance; }
    public void setExpected_closing_balance(BigDecimal expected_closing_balance) { this.expected_closing_balance = expected_closing_balance; }

    public BigDecimal getActual_closing_balance() { return actual_closing_balance; }
    public void setActual_closing_balance(BigDecimal actual_closing_balance) { this.actual_closing_balance = actual_closing_balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}