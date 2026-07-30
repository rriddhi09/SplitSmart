package com.splitsmart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splitsmart.entity.Split;

public interface SplitRepository extends JpaRepository<Split, Long> {

    List<Split> findByExpenseId(Long expenseId);

    List<Split> findByExpenseGroupId(Long groupId);
}