// src/main/java/com/pharmacy/dao/interfaces/purchasing/SupplierDAO.java
package com.pharmacy.dao.interfaces.purchasing;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.purchasing.Supplier;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Supplier entity.
 * Updated to support comprehensive partial text search.
 */
public interface SupplierDAO extends GenericDAO<Supplier, Integer> {
    
    // الدوال القديمة
    Optional<Supplier> findByName(String name);

    // الدوال الجديدة (Micro-task 4.1)
    List<Supplier> search(String keyword);
}