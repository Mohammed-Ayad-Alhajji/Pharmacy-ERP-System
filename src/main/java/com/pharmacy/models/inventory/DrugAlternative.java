package com.pharmacy.models.inventory;

public class DrugAlternative {

    private int med_id_1;
    private int med_id_2;

    public DrugAlternative() {
    }

    public DrugAlternative(int med_id_1, int med_id_2) {
        this.med_id_1 = med_id_1;
        this.med_id_2 = med_id_2;
    }

    public int getMed_id_1() { return med_id_1; }
    public void setMed_id_1(int med_id_1) { this.med_id_1 = med_id_1; }

    public int getMed_id_2() { return med_id_2; }
    public void setMed_id_2(int med_id_2) { this.med_id_2 = med_id_2; }
}