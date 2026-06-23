package com.pharmacy.utils.gui;

/**
 * واجهة تُستخدم للكنترولرات التي تحتاج إلى استقبال بيانات قبل فتحها
 */
public interface DataTransferable {
    void receiveData(Object data);
}