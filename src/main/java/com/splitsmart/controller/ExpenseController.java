package com.splitsmart.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.splitsmart.dto.ExpenseRequest;
import com.splitsmart.entity.Expense;
import com.splitsmart.service.ExpenseService;
import com.splitsmart.dto.SettlementResponse;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public Expense addExpense(@RequestBody ExpenseRequest request) {
        return expenseService.addExpense(request);
    }

    @GetMapping("/group/{groupId}")
    public List<Expense> getGroupExpenses(@PathVariable Long groupId) {
        return expenseService.getGroupExpenses(groupId);
    }
    
    @GetMapping("/settlements/{groupId}")
    public List<SettlementResponse> getSettlements(@PathVariable Long groupId) {
        return expenseService.calculateSettlements(groupId);
    }
    
    @PutMapping("/{expenseId}")
    public Expense updateExpense(@PathVariable Long expenseId,
                                 @RequestBody ExpenseRequest request) {
        return expenseService.updateExpense(expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    public String deleteExpense(@PathVariable Long expenseId) {
        return expenseService.deleteExpense(expenseId);
    }
}