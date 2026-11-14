package com.splitsmart.repository;

import com.splitsmart.entity.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    Optional<GroupInvite> findByCode(String code);
    List<GroupInvite> findByGroupId(Long groupId);
    void deleteByGroupId(Long groupId);
}
