// src/main/java/com/pharmacy/dao/interfaces/pos/InsuranceCompanyDAO.java
package com.pharmacy.dao.interfaces.pos;

import com.pharmacy.dao.interfaces.GenericDAO;
import com.pharmacy.models.pos.InsuranceCompany;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for InsuranceCompany entity.
 */
public interface InsuranceCompanyDAO extends GenericDAO<InsuranceCompany, Integer> {
    
    Optional<InsuranceCompany> findByName(String name);
    List<InsuranceCompany> searchByName(String query);
}