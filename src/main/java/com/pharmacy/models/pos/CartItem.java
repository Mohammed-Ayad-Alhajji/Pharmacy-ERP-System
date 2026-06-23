package com.pharmacy.models.pos;

import javafx.beans.property.*;
import java.math.BigDecimal;

public class CartItem {
    private final StringProperty barcode;
    private final StringProperty name;
    private final IntegerProperty batchId;
    private final StringProperty batchNumber;
    private final IntegerProperty maxQuantity; // الكمية المتوفرة في المخزن لهذه الدفعة
    private final StringProperty unit;
    private final ObjectProperty<BigDecimal> price;
    private final IntegerProperty quantity;
    private final ObjectProperty<BigDecimal> insuranceDiscount;
    private final ObjectProperty<BigDecimal> subTotal;
    
    // تم التصحيح: إزالة "= null" للسماح بالتهيئة داخل الـ Constructor
    private final BooleanProperty prescriptionRequired; 

    // تم التصحيح: إضافة "boolean prescriptionRequired" كمعلمة أخيرة
    public CartItem(String barcode, String name, int batchId, String batchNumber, int maxQuantity, String unit, BigDecimal price, int quantity, BigDecimal insuranceDiscount, boolean prescriptionRequired) {
        this.barcode = new SimpleStringProperty(barcode);
        this.name = new SimpleStringProperty(name);
        this.batchId = new SimpleIntegerProperty(batchId);
        this.batchNumber = new SimpleStringProperty(batchNumber);
        this.maxQuantity = new SimpleIntegerProperty(maxQuantity);
        this.unit = new SimpleStringProperty(unit);
        this.price = new SimpleObjectProperty<>(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.insuranceDiscount = new SimpleObjectProperty<>(insuranceDiscount);
        this.subTotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
        
        // بناء الخاصية بشكل صحيح باستخدام المعلمة الممررة
        this.prescriptionRequired = new SimpleBooleanProperty(prescriptionRequired);

        // حساب الإجمالي التلقائي بناءً على الكمية والسعر وخصم التأمين
        updateSubTotal();
        this.quantity.addListener((obs, oldVal, newVal) -> updateSubTotal());
        this.price.addListener((obs, oldVal, newVal) -> updateSubTotal());
        this.insuranceDiscount.addListener((obs, oldVal, newVal) -> updateSubTotal());
    }

    private void updateSubTotal() {
        BigDecimal qty = BigDecimal.valueOf(quantity.get());
        BigDecimal totalBeforeDiscount = price.get().multiply(qty);
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(insuranceDiscount.get().divide(BigDecimal.valueOf(100)));
        this.subTotal.set(totalBeforeDiscount.multiply(discountMultiplier));
    }

    // Getters and Property Methods
    public String getBarcode() { return barcode.get(); }
    public StringProperty barcodeProperty() { return barcode; }
    
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public int getBatchId() { return batchId.get(); }
    
    public String getBatchNumber() { return batchNumber.get(); }
    public StringProperty batchNumberProperty() { return batchNumber; }

    public int getMaxQuantity() { return maxQuantity.get(); }

    public String getUnit() { return unit.get(); }
    public StringProperty unitProperty() { return unit; }

    public BigDecimal getPrice() { return price.get(); }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    public IntegerProperty quantityProperty() { return quantity; }

    public BigDecimal getInsuranceDiscount() { return insuranceDiscount.get(); }
    public ObjectProperty<BigDecimal> insuranceDiscountProperty() { return insuranceDiscount; }

    public BigDecimal getSubTotal() { return subTotal.get(); }
    public ObjectProperty<BigDecimal> subTotalProperty() { return subTotal; }
    
    public boolean isPrescriptionRequired() { return prescriptionRequired.get(); }
    public BooleanProperty prescriptionRequiredProperty() { return prescriptionRequired; }
    public void setPrescriptionRequired(boolean value) { this.prescriptionRequired.set(value); }
}