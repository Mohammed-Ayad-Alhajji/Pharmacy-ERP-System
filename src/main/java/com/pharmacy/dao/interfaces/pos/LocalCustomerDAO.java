// src/main/java/com/pharmacy/dao/interfaces/pos/LocalCustomerDAO.java
package com.pharmacy.dao.interfaces.pos;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.pos.LocalCustomer;
import java.util.List;

/**
 * Data Access Object interface for LocalCustomer entity.
 */
public interface LocalCustomerDAO extends GenericDAO<LocalCustomer, Integer> {
    
    List<LocalCustomer> searchByNameOrPhone(String keyword);
    
    boolean delete(Integer id);
}