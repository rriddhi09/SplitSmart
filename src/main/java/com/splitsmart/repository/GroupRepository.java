package com.splitsmart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.splitsmart.entity.Group;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findDistinctByMembersEmail(String email);
}
