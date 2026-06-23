// مسار الملف: src/main/java/com/pharmacy/services/interfaces/pos/LocalCustomerService.java

package com.pharmacy.services.interfaces.pos;

import com.pharmacy.models.pos.LocalCustomer;

import java.util.List;
import java.util.Optional;

public interface LocalCustomerService {

    Optional<LocalCustomer> createCustomer(LocalCustomer customer);

    boolean updateCustomer(LocalCustomer customer);

    Optional<LocalCustomer> getCustomerById(int id);

    List<LocalCustomer> getAllCustomers();

    List<LocalCustomer> searchCustomers(String keyword);
    
    boolean deleteCustomer(int id);
}