// مسار الملف: src/main/java/com/pharmacy/services/interfaces/purchasing/SupplierService.java

package com.pharmacy.services.interfaces.purchasing;

import com.pharmacy.models.purchasing.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierService {

    Optional<Supplier> createSupplier(Supplier supplier);

    boolean updateSupplier(Supplier supplier);

    boolean deleteSupplier(int supplierId);

    Optional<Supplier> getSupplierById(int supplierId);

    Optional<Supplier> getSupplierByName(String name);

    List<Supplier> getAllSuppliers();

    List<Supplier> searchSuppliers(String keyword);
}