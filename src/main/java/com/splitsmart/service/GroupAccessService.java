package com.splitsmart.service;

import com.splitsmart.entity.Expense;
import com.splitsmart.entity.Group;
import com.splitsmart.entity.User;
import com.splitsmart.repository.ExpenseRepository;
import com.splitsmart.repository.GroupRepository;
import com.splitsmart.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class GroupAccessService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;

    public GroupAccessService(
            UserRepository userRepository,
            GroupRepository groupRepository,
            ExpenseRepository expenseRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public User currentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        User user = userRepository.findByEmail(authentication.getName());

        if (user == null) {
            throw new AccessDeniedException(
                    "Authenticated user no longer exists"
            );
        }

        return user;
    }

    /*
     * These boolean methods are used by @PreAuthorize.
     * Returning false causes Spring Security to return 403 Forbidden.
     */

    @Transactional(readOnly = true)
    public boolean isMember(Long groupId, String email) {
        Group group = groupRepository.findById(groupId).orElse(null);
        User user = userRepository.findByEmail(email);

        return group != null
                && user != null
                && isMember(group, user);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long groupId, String email) {
        Group group = groupRepository.findById(groupId).orElse(null);
        User user = userRepository.findByEmail(email);

        return group != null
                && user != null
                && group.getOwner() != null
                && group.getOwner().getId().equals(user.getId());
    }

    @Transactional(readOnly = true)
    public boolean canViewExpense(Long expenseId, String email) {
        Expense expense = expenseRepository.findById(expenseId).orElse(null);

        return expense != null
                && isMember(expense.getGroup(), userRepository.findByEmail(email));
    }

    @Transactional(readOnly = true)
    public boolean canManageExpense(Long expenseId, String email) {
        Expense expense = expenseRepository.findById(expenseId).orElse(null);
        User user = userRepository.findByEmail(email);

        if (expense == null || user == null) {
            return false;
        }

        boolean createdExpense = expense.getCreatedBy() != null
                && expense.getCreatedBy().getId().equals(user.getId());

        boolean ownsGroup = expense.getGroup().getOwner() != null
                && expense.getGroup().getOwner().getId().equals(user.getId());

        return createdExpense || ownsGroup;
    }

    /*
     * Existing explicit checks remain useful inside service code.
     */

    @Transactional(readOnly = true)
    public Group requireMember(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        requireMember(group, currentUser());
        return group;
    }

    public void requireMember(Group group, User user) {
        if (!isMember(group, user)) {
            throw new AccessDeniedException(
                    "You are not a member of this group"
            );
        }
    }

    public void requireOwner(Group group, User user) {
        if (group.getOwner() == null
                || !group.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "Only the group owner may perform this action"
            );
        }
    }

    public void requireExpenseManager(Expense expense, User user) {
        boolean createdExpense = expense.getCreatedBy() != null
                && expense.getCreatedBy().getId().equals(user.getId());

        boolean ownsGroup = expense.getGroup().getOwner() != null
                && expense.getGroup().getOwner().getId().equals(user.getId());

        if (!createdExpense && !ownsGroup) {
            throw new AccessDeniedException(
                    "Only the expense creator or group owner may perform this action"
            );
        }
    }

    private boolean isMember(Group group, User user) {
        if (group == null || user == null) {
            return false;
        }

        return group.getMembers().stream()
                .anyMatch(member -> member.getId().equals(user.getId()));
    }
}