// مسار الملف: src/main/java/com/pharmacy/services/impl/finance/ExpenseServiceImpl.java

package com.pharmacy.services.impl.finance;

import com.pharmacy.dao.interfaces.finance.ExpenseDAO;
import com.pharmacy.models.finance.Expense;
import com.pharmacy.services.interfaces.finance.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseDAO expenseDAO;

    public ExpenseServiceImpl(ExpenseDAO expenseDAO) {
        this.expenseDAO = expenseDAO;
    }

    @Override
    public Optional<Expense> createExpense(Expense expense) {
        if (expense.getCategory_id() <= 0) {
            throw new IllegalArgumentException("يجب تحديد تصنيف المصروف");
        }

        if (expense.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن يرتبط المصروف بوردية مفتوحة");
        }

        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("قيمة المصروف يجب أن تكون أكبر من الصفر");
        }

        if (expense.getExpense_date() == null) {
            expense.setExpense_date(LocalDateTime.now());
        }

        return expenseDAO.create(expense);
    }

    @Override
    public boolean updateExpense(Expense expense) {
        if (expense.getCategory_id() <= 0) {
            throw new IllegalArgumentException("يجب تحديد تصنيف المصروف");
        }

        if (expense.getShift_id() <= 0) {
            throw new IllegalArgumentException("يجب أن يرتبط المصروف بوردية مفتوحة");
        }

        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("قيمة المصروف يجب أن تكون أكبر من الصفر");
        }

        return expenseDAO.update(expense);
    }

    @Override
    public boolean deleteExpense(int expenseId) {
        // حذفه يجب أن يكون ممكناً طالما أنه لا يخل بأي قيود أخرى، 
        // وفي حال وجود متطلبات رقابية، يفضل أن تقوم واجهة المستخدم بمنع الحذف وتوفير "تعديل معاكس".
        return expenseDAO.delete(expenseId);
    }

    @Override
    public Optional<Expense> getExpenseById(int id) {
        return expenseDAO.findById(id);
    }

    @Override
    public List<Expense> getExpensesByCategory(int categoryId) {
        return expenseDAO.findByCategoryId(categoryId);
    }

    @Override
    public List<Expense> getExpensesByShift(int shiftId) {
        return expenseDAO.findByShiftId(shiftId);
    }

    @Override
    public List<Expense> getExpensesByDateRange(LocalDate start, LocalDate end) {
        return expenseDAO.findByDateRange(start, end);
    }
}