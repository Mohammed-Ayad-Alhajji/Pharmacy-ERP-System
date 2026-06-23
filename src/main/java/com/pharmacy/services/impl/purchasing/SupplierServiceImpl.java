// مسار الملف: src/main/java/com/pharmacy/services/impl/purchasing/SupplierServiceImpl.java

package com.pharmacy.services.impl.purchasing;

import com.pharmacy.dao.interfaces.purchasing.SupplierDAO;
import com.pharmacy.models.purchasing.Supplier;
import com.pharmacy.services.interfaces.purchasing.SupplierService;

import java.util.List;
import java.util.Optional;

public class SupplierServiceImpl implements SupplierService {

    private final SupplierDAO supplierDAO;

    public SupplierServiceImpl(SupplierDAO supplierDAO) {
        this.supplierDAO = supplierDAO;
    }

    @Override
    public Optional<Supplier> createSupplier(Supplier supplier) {
        if (supplier == null || supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم المورد مطلوب");
        }

        if (supplierDAO.findByName(supplier.getName()).isPresent()) {
            throw new IllegalArgumentException("يوجد مورد آخر مسجل بنفس هذا الاسم");
        }

        return supplierDAO.create(supplier);
    }

    @Override
    public boolean updateSupplier(Supplier supplier) {
        if (supplier == null || supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم المورد مطلوب");
        }

        Optional<Supplier> existingSupplier = supplierDAO.findByName(supplier.getName());
        if (existingSupplier.isPresent() && existingSupplier.get().getSupplier_id() != supplier.getSupplier_id()) {
            throw new IllegalArgumentException("يوجد مورد آخر مسجل بنفس هذا الاسم");
        }

        return supplierDAO.update(supplier);
    }

    @Override
    public boolean deleteSupplier(int supplierId) {
        try {
            boolean isDeleted = supplierDAO.delete(supplierId);
            if (!isDeleted) {
                throw new IllegalStateException("لا يمكن حذف هذا المورد لوجود فواتير شراء أو دفعات مرتبطة به في النظام.");
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("لا يمكن حذف هذا المورد لوجود فواتير شراء أو دفعات مرتبطة به في النظام.", e);
        }
    }

    @Override
    public Optional<Supplier> getSupplierById(int supplierId) {
        return supplierDAO.findById(supplierId);
    }

    @Override
    public Optional<Supplier> getSupplierByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return supplierDAO.findByName(name);
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierDAO.findAll();
    }

    @Override
    public List<Supplier> searchSuppliers(String keyword) {
        return supplierDAO.search(keyword);
    }
}