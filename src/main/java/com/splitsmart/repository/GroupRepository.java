package com.splitsmart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.splitsmart.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}