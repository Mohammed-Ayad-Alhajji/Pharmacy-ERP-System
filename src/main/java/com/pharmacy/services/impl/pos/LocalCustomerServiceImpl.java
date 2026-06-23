// مسار الملف: src/main/java/com/pharmacy/services/impl/pos/LocalCustomerServiceImpl.java

package com.pharmacy.services.impl.pos;

import com.pharmacy.dao.interfaces.pos.LocalCustomerDAO;
import com.pharmacy.models.pos.LocalCustomer;
import com.pharmacy.services.interfaces.pos.LocalCustomerService;

import java.util.List;
import java.util.Optional;

public class LocalCustomerServiceImpl implements LocalCustomerService {

    private final LocalCustomerDAO localCustomerDAO;

    public LocalCustomerServiceImpl(LocalCustomerDAO localCustomerDAO) {
        this.localCustomerDAO = localCustomerDAO;
    }

    @Override
    public Optional<LocalCustomer> createCustomer(LocalCustomer customer) {
        if (customer == null || customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم العميل مطلوب");
        }
        return localCustomerDAO.create(customer);
    }

    @Override
    public boolean updateCustomer(LocalCustomer customer) {
        if (customer == null || customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم العميل مطلوب");
        }
        return localCustomerDAO.update(customer);
    }

    @Override
    public Optional<LocalCustomer> getCustomerById(int id) {
        return localCustomerDAO.findById(id);
    }

    @Override
    public List<LocalCustomer> getAllCustomers() {
        return localCustomerDAO.findAll();
    }

    @Override
    public List<LocalCustomer> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return localCustomerDAO.findAll();
        }
        return localCustomerDAO.searchByNameOrPhone(keyword);
    }
    
    @Override
    public boolean deleteCustomer(int id) {
        return localCustomerDAO.delete(id);
    }
}