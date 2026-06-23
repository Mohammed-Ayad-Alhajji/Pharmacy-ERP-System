package com.pharmacy.models.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Expense {

    private int expense_id;
    private int category_id;
    private int shift_id;
    private BigDecimal amount;
    private LocalDateTime expense_date;
    private String description;

    public Expense() {
    }

    public Expense(int category_id, int shift_id, BigDecimal amount, 
                   LocalDateTime expense_date, String description) {
        this.category_id = category_id;
        this.shift_id = shift_id;
        this.amount = amount;
        this.expense_date = expense_date;
        this.description = description;
    }

    public int getExpense_id() { return expense_id; }
    public void setExpense_id(int expense_id) { this.expense_id = expense_id; }

    public int getCategory_id() { return category_id; }
    public void setCategory_id(int category_id) { this.category_id = category_id; }

    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getExpense_date() { return expense_date; }
    public void setExpense_date(LocalDateTime expense_date) { this.expense_date = expense_date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}