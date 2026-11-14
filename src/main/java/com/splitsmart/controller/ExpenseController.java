package com.splitsmart.controller;

import com.splitsmart.dto.ExpenseRequest;
import com.splitsmart.dto.SettlementResponse;
import com.splitsmart.entity.Expense;
import com.splitsmart.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expense addExpense(@RequestBody ExpenseRequest request) {
        return expenseService.addExpense(request);
    }

    @GetMapping("/{expenseId}")
    public Expense getExpense(@PathVariable Long expenseId) { return expenseService.getExpense(expenseId); }

    @GetMapping("/group/{groupId}")
    public List<Expense> getGroupExpenses(@PathVariable Long groupId) {
        return expenseService.getGroupExpenses(groupId);
    }

    @GetMapping("/settlements/{groupId}")
    public List<SettlementResponse> getSettlements(@PathVariable Long groupId) {
        return expenseService.calculateSettlements(groupId);
    }

    @PutMapping("/{expenseId}")
    public Expense updateExpense(@PathVariable Long expenseId, @RequestBody ExpenseRequest request) {
        return expenseService.updateExpense(expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long expenseId) { expenseService.deleteExpense(expenseId); }
}
