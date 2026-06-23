// مسار الملف: src/main/java/com/pharmacy/services/interfaces/pos/PatientReturnService.java

package com.pharmacy.services.interfaces.pos;

import com.pharmacy.models.pos.PatientReturn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientReturnService {

    Optional<PatientReturn> createReturn(PatientReturn patientReturn);

    boolean updateReturn(PatientReturn patientReturn);

    Optional<PatientReturn> getReturnById(int returnId);

    List<PatientReturn> getReturnsBySaleDetail(int detailId);

    List<PatientReturn> getReturnsByShift(int shiftId);

    List<PatientReturn> getReturnsByDateRange(LocalDate start, LocalDate end);
}