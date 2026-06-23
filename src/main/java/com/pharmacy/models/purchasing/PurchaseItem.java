package com.pharmacy.models.purchasing;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * نموذج بيانات يمثل سطراً واحداً في جدول فاتورة المشتريات (In-Memory Model).
 * يستخدم الـ Properties لربطه مباشرة مع واجهة JavaFX بشكل ديناميكي.
 */
public class PurchaseItem {

    private final StringProperty barcode;
    private final StringProperty name;
    private final StringProperty batchNumber;
    private final ObjectProperty<LocalDate> mfgDate;
    private final ObjectProperty<LocalDate> expDate;
    private final IntegerProperty quantity;
    private final IntegerProperty bonus;
    private final ObjectProperty<BigDecimal> boxCost;
    private final ObjectProperty<BigDecimal> totalCost;

    // معرّف الدواء في قاعدة البيانات (غير ظاهر في الجدول ولكنه مهم للحفظ)
    private final int medId;

    public PurchaseItem(int medId, String barcode, String name, String batchNumber, LocalDate mfgDate, LocalDate expDate, int quantity, int bonus, BigDecimal boxCost) {
        this.medId = medId;
        this.barcode = new SimpleStringProperty(barcode);
        this.name = new SimpleStringProperty(name);
        this.batchNumber = new SimpleStringProperty(batchNumber);
        this.mfgDate = new SimpleObjectProperty<>(mfgDate);
        this.expDate = new SimpleObjectProperty<>(expDate);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.bonus = new SimpleIntegerProperty(bonus);
        this.boxCost = new SimpleObjectProperty<>(boxCost);
        this.totalCost = new SimpleObjectProperty<>(BigDecimal.ZERO);

        // حساب الإجمالي تلقائياً (الكمية المشترات الأساسية × سعر الشراء للعلبة)
        // ملاحظة: البونص (المجاني) لا يدخل في حساب إجمالي التكلفة
        calculateTotal();

        // مستمعات (Listeners) لإعادة حساب الإجمالي فور تعديل الكمية أو السعر من الجدول
        this.quantity.addListener((obs, oldVal, newVal) -> calculateTotal());
        this.boxCost.addListener((obs, oldVal, newVal) -> calculateTotal());
    }

    private void calculateTotal() {
        if (boxCost.get() != null) {
            BigDecimal total = boxCost.get().multiply(BigDecimal.valueOf(quantity.get()));
            totalCost.set(total);
        }
    }

    // --- Getters & Setters & Property Methods ---

    public int getMedId() { return medId; }

    public String getBarcode() { return barcode.get(); }
    public void setBarcode(String value) { barcode.set(value); }
    public StringProperty barcodeProperty() { return barcode; }

    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    public String getBatchNumber() { return batchNumber.get(); }
    public void setBatchNumber(String value) { batchNumber.set(value); }
    public StringProperty batchNumberProperty() { return batchNumber; }

    public LocalDate getMfgDate() { return mfgDate.get(); }
    public void setMfgDate(LocalDate value) { mfgDate.set(value); }
    public ObjectProperty<LocalDate> mfgDateProperty() { return mfgDate; }

    public LocalDate getExpDate() { return expDate.get(); }
    public void setExpDate(LocalDate value) { expDate.set(value); }
    public ObjectProperty<LocalDate> expDateProperty() { return expDate; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int value) { quantity.set(value); }
    public IntegerProperty quantityProperty() { return quantity; }

    public int getBonus() { return bonus.get(); }
    public void setBonus(int value) { bonus.set(value); }
    public IntegerProperty bonusProperty() { return bonus; }

    public BigDecimal getBoxCost() { return boxCost.get(); }
    public void setBoxCost(BigDecimal value) { boxCost.set(value); }
    public ObjectProperty<BigDecimal> boxCostProperty() { return boxCost; }

    public BigDecimal getTotalCost() { return totalCost.get(); }
    public ObjectProperty<BigDecimal> totalCostProperty() { return totalCost; }
}