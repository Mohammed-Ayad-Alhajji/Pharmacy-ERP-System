// مسار الملف: src/main/java/com/pharmacy/services/interfaces/pos/InsuranceCompanyService.java

package com.pharmacy.services.interfaces.pos;

import com.pharmacy.models.pos.InsuranceCompany;

import java.util.List;
import java.util.Optional;

public interface InsuranceCompanyService {

    Optional<InsuranceCompany> createCompany(InsuranceCompany company);

    boolean updateCompany(InsuranceCompany company);

    Optional<InsuranceCompany> getCompanyById(int id);

    Optional<InsuranceCompany> getCompanyByName(String name);

    List<InsuranceCompany> getAllCompanies();
    // أضف هذه الدالة إلى الواجهة
    boolean deleteCompany(int id);
}