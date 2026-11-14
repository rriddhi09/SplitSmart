package com.splitsmart.service;

import com.splitsmart.dto.ExpenseRequest;
import com.splitsmart.dto.SettlementResponse;
import com.splitsmart.entity.Expense;
import com.splitsmart.entity.Group;
import com.splitsmart.entity.Split;
import com.splitsmart.entity.User;
import com.splitsmart.repository.ExpenseRepository;
import com.splitsmart.repository.SplitRepository;
import com.splitsmart.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final SplitRepository splitRepository;
    private final UserRepository userRepository;
    private final GroupAccessService groupAccessService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          SplitRepository splitRepository,
                          UserRepository userRepository,
                          GroupAccessService groupAccessService) {
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.userRepository = userRepository;
        this.groupAccessService = groupAccessService;
    }

    @Transactional
    @PreAuthorize("@groupAccessService.isMember(#request.groupId, authentication.name)")
    public Expense addExpense(ExpenseRequest request) {
        validateRequest(request);
        Group group = groupAccessService.requireMember(request.getGroupId());
        User currentUser = groupAccessService.currentUser();
        User paidByUser = findUser(request.getPaidByUserId());
        requireMember(group, paidByUser, "The payer must be a group member");

        List<User> participants = new ArrayList<>(group.getMembers());
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("A group must have at least one member before adding an expense");
        }

        Expense expense = new Expense();
        expense.setDescription(request.getDescription().trim());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidByUser);
        expense.setGroup(group);
        expense.setCreatedBy(currentUser);
        Expense savedExpense = expenseRepository.save(expense);
        createSplits(savedExpense, participants);
        return savedExpense;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@groupAccessService.canViewExpense(#expenseId, authentication.name)")
    public Expense getExpense(Long expenseId) {
        Expense expense = findExpense(expenseId);
        groupAccessService.requireMember(expense.getGroup(), groupAccessService.currentUser());
        return expense;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@groupAccessService.isMember(#groupId, authentication.name)")
    public List<Expense> getGroupExpenses(Long groupId) {
        groupAccessService.requireMember(groupId);
        return expenseRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@groupAccessService.isMember(#groupId, authentication.name)")
    public List<SettlementResponse> calculateSettlements(Long groupId) {
        groupAccessService.requireMember(groupId);
        List<Split> splits = splitRepository.findByExpenseGroupId(groupId);
        Map<User, Double> balances = new HashMap<>();

        for (Split split : splits) {
            double net = split.getAmountPaid() - split.getAmountOwed();
            balances.put(split.getUser(), balances.getOrDefault(split.getUser(), 0.0) + net);
        }

        List<Map.Entry<User, Double>> creditors = new ArrayList<>();
        List<Map.Entry<User, Double>> debtors = new ArrayList<>();
        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            if (entry.getValue() > 0.0001) {
                creditors.add(entry);
            } else if (entry.getValue() < -0.0001) {
                debtors.add(entry);
            }
        }

        List<SettlementResponse> result = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;
        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            Map.Entry<User, Double> debtor = debtors.get(debtorIndex);
            Map.Entry<User, Double> creditor = creditors.get(creditorIndex);
            double settledAmount = Math.min(-debtor.getValue(), creditor.getValue());

            result.add(new SettlementResponse(
                    debtor.getKey().getName(), creditor.getKey().getName(), settledAmount));
            debtor.setValue(debtor.getValue() + settledAmount);
            creditor.setValue(creditor.getValue() - settledAmount);
            if (Math.abs(debtor.getValue()) < 0.0001) debtorIndex++;
            if (Math.abs(creditor.getValue()) < 0.0001) creditorIndex++;
        }
        return result;
    }

    @Transactional
    @PreAuthorize("@groupAccessService.canManageExpense(#expenseId, authentication.name)")
    public Expense updateExpense(Long expenseId, ExpenseRequest request) {
        validateRequest(request);
        Expense expense = findExpense(expenseId);
        User currentUser = groupAccessService.currentUser();
        groupAccessService.requireExpenseManager(expense, currentUser);

        if (!expense.getGroup().getId().equals(request.getGroupId())) {
            throw new IllegalArgumentException("An expense cannot be moved to another group");
        }
        User paidByUser = findUser(request.getPaidByUserId());
        requireMember(expense.getGroup(), paidByUser, "The payer must be a group member");

        // Keep the original split participants so future joiners never inherit old expenses.
        List<Split> oldSplits = splitRepository.findByExpenseId(expenseId);
        List<User> participants = oldSplits.stream().map(Split::getUser).toList();
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("Expense has no split participants");
        }

        expense.setDescription(request.getDescription().trim());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidByUser);
        splitRepository.deleteAll(oldSplits);
        createSplits(expense, participants);
        return expense;
    }

    @Transactional
    @PreAuthorize("@groupAccessService.canManageExpense(#expenseId, authentication.name)")
    public void deleteExpense(Long expenseId) {
        Expense expense = findExpense(expenseId);
        groupAccessService.requireExpenseManager(expense, groupAccessService.currentUser());
        splitRepository.deleteAll(splitRepository.findByExpenseId(expenseId));
        expenseRepository.delete(expense);
    }

    private void createSplits(Expense expense, List<User> participants) {
        double splitAmount = expense.getAmount() / participants.size();
        for (User user : participants) {
            Split split = new Split();
            split.setExpense(expense);
            split.setUser(user);
            split.setAmountOwed(splitAmount);
            split.setAmountPaid(user.getId().equals(expense.getPaidBy().getId()) ? expense.getAmount() : 0.0);
            splitRepository.save(split);
        }
    }

    private Expense findExpense(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException("Expense not found"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private void requireMember(Group group, User user, String message) {
        boolean member = group.getMembers().stream().anyMatch(candidate -> candidate.getId().equals(user.getId()));
        if (!member) throw new IllegalArgumentException(message);
    }

    private void validateRequest(ExpenseRequest request) {
        if (request == null || request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Expense description is required");
        }
        if (request.getAmount() == null || !Double.isFinite(request.getAmount()) || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Expense amount must be positive");
        }
        if (request.getGroupId() == null || request.getPaidByUserId() == null) {
            throw new IllegalArgumentException("Group id and payer id are required");
        }
    }
}
