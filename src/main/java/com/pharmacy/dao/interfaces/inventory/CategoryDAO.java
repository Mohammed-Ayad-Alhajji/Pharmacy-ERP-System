// src/main/java/com/pharmacy/dao/interfaces/inventory/CategoryDAO.java
package com.pharmacy.dao.interfaces.inventory;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.inventory.Category;
import java.util.Optional;

/**
 * Data Access Object interface for Category entity.
 */
public interface CategoryDAO extends GenericDAO<Category, Integer> {
    Optional<Category> findByName(String name);
}