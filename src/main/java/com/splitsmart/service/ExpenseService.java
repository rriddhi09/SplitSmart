package com.splitsmart.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.splitsmart.dto.SettlementResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.splitsmart.dto.ExpenseRequest;
import com.splitsmart.entity.Expense;
import com.splitsmart.entity.Group;
import com.splitsmart.entity.Split;
import com.splitsmart.entity.User;
import com.splitsmart.repository.ExpenseRepository;
import com.splitsmart.repository.GroupRepository;
import com.splitsmart.repository.SplitRepository;
import com.splitsmart.repository.UserRepository;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SplitRepository splitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    public Expense addExpense(ExpenseRequest request) {

        User paidByUser = userRepository.findById(request.getPaidByUserId()).orElseThrow();
        Group group = groupRepository.findById(request.getGroupId()).orElseThrow();

        Expense expense = new Expense();
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidByUser);
        expense.setGroup(group);

        Expense savedExpense = expenseRepository.save(expense);

        List<User> members = group.getMembers();
        double splitAmount = request.getAmount() / members.size();

        for (User user : members) {
            Split split = new Split();
            split.setExpense(savedExpense);
            split.setUser(user);
            split.setAmountOwed(splitAmount);

            if (user.getId().equals(paidByUser.getId())) {
                split.setAmountPaid(request.getAmount());
            } else {
                split.setAmountPaid(0.0);
            }

            splitRepository.save(split);
        }

        return savedExpense;
    }

    public List<Expense> getGroupExpenses(Long groupId) {
        return expenseRepository.findByGroupId(groupId);
    }
    
    public List<SettlementResponse> calculateSettlements(Long groupId) {

        List<Split> splits = splitRepository.findByExpenseGroupId(groupId);

        Map<User, Double> balanceMap = new HashMap<>();

        for (Split split : splits) {
            double paid = split.getAmountPaid();
            double owed = split.getAmountOwed();
            double net = paid - owed;

            balanceMap.put(split.getUser(),
                    balanceMap.getOrDefault(split.getUser(), 0.0) + net);
        }

        List<Map.Entry<User, Double>> creditors = new ArrayList<>();
        List<Map.Entry<User, Double>> debtors = new ArrayList<>();

        for (Map.Entry<User, Double> entry : balanceMap.entrySet()) {
            double value = entry.getValue();

            if (value > 0.0) {
                creditors.add(entry);
            } else if (value < 0.0) {
                debtors.add(entry);
            }
        }

        List<SettlementResponse> result = new ArrayList<>();

        int i = 0;
        int j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<User, Double> debtor = debtors.get(i);
            Map.Entry<User, Double> creditor = creditors.get(j);

            double debtAmount = -debtor.getValue();
            double creditAmount = creditor.getValue();

            double settledAmount = Math.min(debtAmount, creditAmount);

            result.add(new SettlementResponse(
                    debtor.getKey().getName(),
                    creditor.getKey().getName(),
                    settledAmount
            ));

            debtor.setValue(debtor.getValue() + settledAmount);
            creditor.setValue(creditor.getValue() - settledAmount);

            if (Math.abs(debtor.getValue()) < 0.0001) {
                i++;
            }

            if (Math.abs(creditor.getValue()) < 0.0001) {
                j++;
            }
        }

        return result;
    }
    
    public Expense updateExpense(Long expenseId, ExpenseRequest request) {

        Expense expense = expenseRepository.findById(expenseId).orElseThrow();

        User paidByUser = userRepository.findById(request.getPaidByUserId()).orElseThrow();
        Group group = groupRepository.findById(request.getGroupId()).orElseThrow();

        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidByUser);
        expense.setGroup(group);

        Expense updatedExpense = expenseRepository.save(expense);

        // delete old split rows of this expense
        List<Split> oldSplits = splitRepository.findByExpenseId(expenseId);
        splitRepository.deleteAll(oldSplits);

        // create fresh split rows
        List<User> members = group.getMembers();
        double splitAmount = request.getAmount() / members.size();

        for (User user : members) {
            Split split = new Split();
            split.setExpense(updatedExpense);
            split.setUser(user);
            split.setAmountOwed(splitAmount);

            if (user.getId().equals(paidByUser.getId())) {
                split.setAmountPaid(request.getAmount());
            } else {
                split.setAmountPaid(0.0);
            }

            splitRepository.save(split);
        }

        return updatedExpense;
    }

    public String deleteExpense(Long expenseId) {

        List<Split> splits = splitRepository.findByExpenseId(expenseId);
        splitRepository.deleteAll(splits);

        expenseRepository.deleteById(expenseId);

        return "Expense deleted successfully";
    }
}