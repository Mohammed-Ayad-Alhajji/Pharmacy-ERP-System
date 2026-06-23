// مسار الملف: src/main/java/com/pharmacy/services/impl/pos/PatientReturnServiceImpl.java

package com.pharmacy.services.impl.pos;

import com.pharmacy.dao.interfaces.pos.PatientReturnDAO;
import com.pharmacy.models.pos.PatientReturn;
import com.pharmacy.services.interfaces.pos.PatientReturnService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PatientReturnServiceImpl implements PatientReturnService {

    private final PatientReturnDAO patientReturnDAO;

    public PatientReturnServiceImpl(PatientReturnDAO patientReturnDAO) {
        this.patientReturnDAO = patientReturnDAO;
    }

    private void validateAmountsAndQuantities(PatientReturn patientReturn) {
        if (patientReturn.getQuantity_returned() <= 0) {
            throw new IllegalArgumentException("الكمية المرتجعة يجب أن تكون أكبر من الصفر");
        }

        if (patientReturn.getPatient_cash_refund() == null) {
            patientReturn.setPatient_cash_refund(BigDecimal.ZERO);
        } else if (patientReturn.getPatient_cash_refund().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("المبلغ المرتجع للمريض لا يمكن أن يكون سالباً");
        }

        if (patientReturn.getInsurance_canceled_amount() == null) {
            patientReturn.setInsurance_canceled_amount(BigDecimal.ZERO);
        } else if (patientReturn.getInsurance_canceled_amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("مبلغ التأمين الملغى لا يمكن أن يكون سالباً");
        }
    }

    @Override
    public Optional<PatientReturn> createReturn(PatientReturn patientReturn) {
        if (patientReturn.getDetail_id() <= 0) {
            throw new IllegalArgumentException("يجب تحديد الدواء المباع (سطر الفاتورة) المراد إرجاعه");
        }

        if (patientReturn.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن يرتبط المرتجع بوردية مفتوحة");
        }

        validateAmountsAndQuantities(patientReturn);

        if (patientReturn.getReturn_date() == null) {
            patientReturn.setReturn_date(LocalDateTime.now());
        }

        return patientReturnDAO.create(patientReturn);
    }

    @Override
    public boolean updateReturn(PatientReturn patientReturn) {
        validateAmountsAndQuantities(patientReturn);
        return patientReturnDAO.update(patientReturn);
    }

    @Override
    public Optional<PatientReturn> getReturnById(int returnId) {
        return patientReturnDAO.findById(returnId);
    }

    @Override
    public List<PatientReturn> getReturnsBySaleDetail(int detailId) {
        return patientReturnDAO.findBySaleDetailId(detailId);
    }

    @Override
    public List<PatientReturn> getReturnsByShift(int shiftId) {
        return patientReturnDAO.findByShiftId(shiftId);
    }

    @Override
    public List<PatientReturn> getReturnsByDateRange(LocalDate start, LocalDate end) {
        return patientReturnDAO.findByDateRange(start, end);
    }
}