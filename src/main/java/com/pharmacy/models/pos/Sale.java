package com.pharmacy.models.pos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sale {

    private int sale_id;
    private int shift_id;
    private Integer customer_id;
    private Integer insurance_id;
    private String insurance_approval_code;
    private String prescription_image_path;
    private LocalDateTime sale_date;
    private BigDecimal total_amount;
    private BigDecimal discount_amount;
    private BigDecimal rounding_adjustment;
    private BigDecimal total_patient_paid;
    private BigDecimal total_customer_debt;
    private BigDecimal total_insurance_debt;
    private String payment_method;
    private String status;
    private String doctor_name;
    private String patient_name;

    public Sale() {
    }

    public Sale(int shift_id, Integer customer_id, Integer insurance_id, String insurance_approval_code, 
                String prescription_image_path, LocalDateTime sale_date, BigDecimal total_amount, 
                BigDecimal discount_amount, BigDecimal rounding_adjustment, BigDecimal total_patient_paid, 
                BigDecimal total_customer_debt, BigDecimal total_insurance_debt, String payment_method, 
                String status) {
        this.shift_id = shift_id;
        this.customer_id = customer_id;
        this.insurance_id = insurance_id;
        this.insurance_approval_code = insurance_approval_code;
        this.prescription_image_path = prescription_image_path;
        this.sale_date = sale_date;
        this.total_amount = total_amount;
        this.discount_amount = discount_amount;
        this.rounding_adjustment = rounding_adjustment;
        this.total_patient_paid = total_patient_paid;
        this.total_customer_debt = total_customer_debt;
        this.total_insurance_debt = total_insurance_debt;
        this.payment_method = payment_method;
        this.status = status;
    }
    
    
    public int getSale_id() { return sale_id; }
    public void setSale_id(int sale_id) { this.sale_id = sale_id; }

    public int getShift_id() { return shift_id; }
    public void setShift_id(int shift_id) { this.shift_id = shift_id; }

    public Integer getCustomer_id() { return customer_id; }
    public void setCustomer_id(Integer customer_id) { this.customer_id = customer_id; }

    public Integer getInsurance_id() { return insurance_id; }
    public void setInsurance_id(Integer insurance_id) { this.insurance_id = insurance_id; }

    public String getInsurance_approval_code() { return insurance_approval_code; }
    public void setInsurance_approval_code(String insurance_approval_code) { this.insurance_approval_code = insurance_approval_code; }

    public String getPrescription_image_path() { return prescription_image_path; }
    public void setPrescription_image_path(String prescription_image_path) { this.prescription_image_path = prescription_image_path; }

    public LocalDateTime getSale_date() { return sale_date; }
    public void setSale_date(LocalDateTime sale_date) { this.sale_date = sale_date; }

    public BigDecimal getTotal_amount() { return total_amount; }
    public void setTotal_amount(BigDecimal total_amount) { this.total_amount = total_amount; }

    public BigDecimal getDiscount_amount() { return discount_amount; }
    public void setDiscount_amount(BigDecimal discount_amount) { this.discount_amount = discount_amount; }

    public BigDecimal getRounding_adjustment() { return rounding_adjustment; }
    public void setRounding_adjustment(BigDecimal rounding_adjustment) { this.rounding_adjustment = rounding_adjustment; }

    public BigDecimal getTotal_patient_paid() { return total_patient_paid; }
    public void setTotal_patient_paid(BigDecimal total_patient_paid) { this.total_patient_paid = total_patient_paid; }

    public BigDecimal getTotal_customer_debt() { return total_customer_debt; }
    public void setTotal_customer_debt(BigDecimal total_customer_debt) { this.total_customer_debt = total_customer_debt; }

    public BigDecimal getTotal_insurance_debt() { return total_insurance_debt; }
    public void setTotal_insurance_debt(BigDecimal total_insurance_debt) { this.total_insurance_debt = total_insurance_debt; }

    public String getPayment_method() { return payment_method; }
    public void setPayment_method(String payment_method) { this.payment_method = payment_method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDoctor_name() { return doctor_name; }
    public void setDoctor_name(String doctor_name) { this.doctor_name = doctor_name; }
    
    public String getPatient_name() { return patient_name; }
    public void setPatient_name(String patient_name) { this.patient_name = patient_name; }
}