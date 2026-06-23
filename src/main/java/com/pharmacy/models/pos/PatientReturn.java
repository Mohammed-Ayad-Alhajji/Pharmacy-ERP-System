package com.pharmacy.models.pos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PatientReturn {

    private int return_id;
    private int detail_id;
    private int shift_id;
    private int quantity_returned;
    private BigDecimal patient_cash_refund;
    private BigDecimal insurance_canceled_amount;
    private LocalDateTime return_date;
    private String reason;

    public PatientReturn() {
    }

    public PatientReturn(int detail_id, int shift_id, int quantity_returned, 
                         BigDecimal patient_cash_refund, BigDecimal insurance_canceled_amount, 
                         LocalDateTime return_date, String reason) {
        this.detail_id = detail_id;
        this.shift_id = shift_id;
        this.quantity_returned = quantity_returned;
        this.patient_cash_refund = patient_cash_refund;
        this.insurance_canceled_amount = insurance_canceled_amount;
        this.return_date = return_date;
        this.reason = reason;
    }

    public int getReturn_id() { return return_id; }
    public void setReturn_id(int return_id) { this.return_id = return_id; }

    public int getDetail_id() { return detail_id; }
    public void setDetail_id(int detail_id) { this.detail_id = detail_id; }

    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public int getQuantity_returned() { return quantity_returned; }
    public void setQuantity_returned(int quantity_returned) { this.quantity_returned = quantity_returned; }

    public BigDecimal getPatient_cash_refund() { return patient_cash_refund; }
    public void setPatient_cash_refund(BigDecimal patient_cash_refund) { this.patient_cash_refund = patient_cash_refund; }

    public BigDecimal getInsurance_canceled_amount() { return insurance_canceled_amount; }
    public void setInsurance_canceled_amount(BigDecimal insurance_canceled_amount) { this.insurance_canceled_amount = insurance_canceled_amount; }

    public LocalDateTime getReturn_date() { return return_date; }
    public void setReturn_date(LocalDateTime return_date) { this.return_date = return_date; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}