// مسار الملف: src/main/java/com/pharmacy/services/impl/pos/InsuranceCompanyServiceImpl.java

package com.pharmacy.services.impl.pos;

import com.pharmacy.dao.interfaces.pos.InsuranceCompanyDAO;
import com.pharmacy.models.pos.InsuranceCompany;
import com.pharmacy.services.interfaces.pos.InsuranceCompanyService;

import java.util.List;
import java.util.Optional;

public class InsuranceCompanyServiceImpl implements InsuranceCompanyService {

    private final InsuranceCompanyDAO insuranceCompanyDAO;

    public InsuranceCompanyServiceImpl(InsuranceCompanyDAO insuranceCompanyDAO) {
        this.insuranceCompanyDAO = insuranceCompanyDAO;
    }

    @Override
    public Optional<InsuranceCompany> createCompany(InsuranceCompany company) {
        if (company == null || company.getName() == null || company.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم شركة التأمين مطلوب");
        }

        if (insuranceCompanyDAO.findByName(company.getName()).isPresent()) {
            throw new IllegalArgumentException("هذه الشركة مسجلة مسبقاً");
        }

        return insuranceCompanyDAO.create(company);
    }

    @Override
    public boolean updateCompany(InsuranceCompany company) {
        if (company == null || company.getName() == null || company.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("اسم شركة التأمين مطلوب");
        }

        Optional<InsuranceCompany> existingCompany = insuranceCompanyDAO.findByName(company.getName());
        if (existingCompany.isPresent() && existingCompany.get().getInsurance_id() != company.getInsurance_id()) {
            throw new IllegalArgumentException("هذه الشركة مسجلة مسبقاً");
        }

        return insuranceCompanyDAO.update(company);
    }

    @Override
    public Optional<InsuranceCompany> getCompanyById(int id) {
        return insuranceCompanyDAO.findById(id);
    }

    @Override
    public Optional<InsuranceCompany> getCompanyByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return insuranceCompanyDAO.findByName(name);
    }

    @Override
    public List<InsuranceCompany> getAllCompanies() {
        return insuranceCompanyDAO.findAll();
    }
    // أضف هذه الدالة داخل الكلاس
    @Override
    public boolean deleteCompany(int id) {
        return insuranceCompanyDAO.delete(id);
    }
}