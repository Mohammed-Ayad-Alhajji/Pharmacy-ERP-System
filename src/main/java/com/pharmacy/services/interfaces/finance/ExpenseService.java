// مسار الملف: src/main/java/com/pharmacy/services/interfaces/finance/ExpenseService.java

package com.pharmacy.services.interfaces.finance;

import com.pharmacy.models.finance.Expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseService {

    Optional<Expense> createExpense(Expense expense);

    boolean updateExpense(Expense expense);

    boolean deleteExpense(int expenseId);

    Optional<Expense> getExpenseById(int id);

    List<Expense> getExpensesByCategory(int categoryId);

    List<Expense> getExpensesByShift(int shiftId);

    List<Expense> getExpensesByDateRange(LocalDate start, LocalDate end);
}