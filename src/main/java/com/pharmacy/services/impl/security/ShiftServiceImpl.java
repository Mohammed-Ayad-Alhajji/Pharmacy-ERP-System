// مسار الملف: src/main/java/com/pharmacy/services/impl/security/ShiftServiceImpl.java

package com.pharmacy.services.impl.security;

import com.pharmacy.dao.interfaces.security.ShiftDAO;
import com.pharmacy.models.finance.ShiftFinancialSummary;
import com.pharmacy.models.security.Shift;
import com.pharmacy.services.interfaces.security.ShiftService;
import com.pharmacy.utils.exceptions.ShiftException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ShiftServiceImpl implements ShiftService {

    private final ShiftDAO shiftDAO;

    public ShiftServiceImpl(ShiftDAO shiftDAO) {
        this.shiftDAO = shiftDAO;
    }

    @Override
    public Shift openShift(int userId, BigDecimal openingBalance) throws ShiftException {
        Optional<Shift> existingOpenShift = shiftDAO.findOpenShiftByUserId(userId);
        if (existingOpenShift.isPresent()) {
            throw new ShiftException("يوجد وردية مفتوحة بالفعل لهذا المستخدم. الرجاء إغلاقها أولاً.");
        }

        Shift newShift = new Shift();
        newShift.setUser_id(userId);
        newShift.setStart_time(LocalDateTime.now());
        newShift.setStatus("Open");
        newShift.setOpening_balance(openingBalance != null ? openingBalance : BigDecimal.ZERO);
        newShift.setExpected_closing_balance(openingBalance != null ? openingBalance : BigDecimal.ZERO);

        return shiftDAO.create(newShift)
                .orElseThrow(() -> new ShiftException("فشل في فتح الوردية."));
    }

    @Override
    public Shift closeShift(int shiftId, BigDecimal actualClosingBalance) throws ShiftException {
        Shift shift = shiftDAO.findById(shiftId)
                .orElseThrow(() -> new ShiftException("الوردية غير موجودة."));

        if (!"Open".equalsIgnoreCase(shift.getStatus())) {
            throw new ShiftException("هذه الوردية مغلقة مسبقاً.");
        }

        // ========================================================
        // اللمسة السحرية: جلب الرصيد المتوقع وحفظه قبل إغلاق الوردية
        // ========================================================
        ShiftFinancialSummary summary = this.getShiftFinancialDetails(shiftId);
        shift.setExpected_closing_balance(summary.expectedBalance);

        // إكمال بيانات الإغلاق
        shift.setEnd_time(LocalDateTime.now());
        shift.setStatus("Closed");
        shift.setActual_closing_balance(actualClosingBalance != null ? actualClosingBalance : BigDecimal.ZERO);

        if (!shiftDAO.update(shift)) {
            throw new ShiftException("فشل في إغلاق الوردية وتحديث قاعدة البيانات.");
        }

        return shift;
    }

    @Override
    public Optional<Shift> getCurrentOpenShift(int userId) {
        return shiftDAO.findOpenShiftByUserId(userId);
    }

    @Override
    public List<Shift> getShiftHistory(int userId, int limit, int offset) {
        return shiftDAO.findByUserId(userId).stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long getShiftHistoryCount(int userId) {
        return shiftDAO.findByUserId(userId).size();
    }

    @Override
    public List<Shift> getShiftsByDateRange(LocalDateTime start, LocalDateTime end, int limit, int offset) {
        return shiftDAO.findByDateRange(start, end).stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Shift> getShiftById(int shiftId) {
        return shiftDAO.getShiftById(shiftId);
    }
    @Override
    public ShiftFinancialSummary getShiftFinancialDetails(int shiftId) {
        // بفرض أن كائن الـ DAO مسمى shiftDAO
        if (shiftDAO instanceof com.pharmacy.dao.impl.security.ShiftDAOImpl) {
            return ((com.pharmacy.dao.impl.security.ShiftDAOImpl) shiftDAO).getShiftFinancialDetails(shiftId);
        }
        return new ShiftFinancialSummary(); 
    }
}