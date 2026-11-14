package com.splitsmart.service;

import com.splitsmart.dto.CreateGroupRequest;
import com.splitsmart.dto.CreateInviteRequest;
import com.splitsmart.dto.UpdateGroupRequest;
import com.splitsmart.dto.TransferOwnershipRequest;
import com.splitsmart.entity.Group;
import com.splitsmart.entity.GroupInvite;
import com.splitsmart.entity.User;
import com.splitsmart.repository.ExpenseRepository;
import com.splitsmart.repository.GroupInviteRepository;
import com.splitsmart.repository.GroupRepository;
import com.splitsmart.repository.SplitRepository;
import com.splitsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class GroupService {

    private static final int DEFAULT_INVITE_EXPIRY_HOURS = 168;
    private static final int MAX_INVITE_EXPIRY_HOURS = 720;

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupInviteRepository inviteRepository;
    private final ExpenseRepository expenseRepository;
    private final SplitRepository splitRepository;
    private final GroupAccessService groupAccessService;

    public GroupService(GroupRepository groupRepository,
                        UserRepository userRepository,
                        GroupInviteRepository inviteRepository,
                        ExpenseRepository expenseRepository,
                        SplitRepository splitRepository,
                        GroupAccessService groupAccessService) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.groupAccessService = groupAccessService;
    }

    @Transactional
    public Group createGroup(CreateGroupRequest request) {
        User owner = groupAccessService.currentUser();
        Group group = new Group();
        group.setName(requiredName(request == null ? null : request.name()));
        group.setOwner(owner);
        group.setMembers(new ArrayList<>(List.of(owner)));
        return groupRepository.save(group);
    }

    @PreAuthorize("@groupAccessService.isMember(#groupId, authentication.name)")
    @Transactional(readOnly = true)
    public Group getGroup(Long groupId) {
        return groupAccessService.requireMember(groupId);
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional
    public Group updateGroup(Long groupId, UpdateGroupRequest request) {
        Group group = groupAccessService.requireMember(groupId);
        groupAccessService.requireOwner(group, groupAccessService.currentUser());
        group.setName(requiredName(request == null ? null : request.name()));
        return group;
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional
    public Group transferOwnership(Long groupId, TransferOwnershipRequest request) {
        if (request == null || request.userId() == null) {
            throw new IllegalArgumentException("New owner user id is required");
        }
        Group group = groupAccessService.requireMember(groupId);
        groupAccessService.requireOwner(group, groupAccessService.currentUser());
        User newOwner = userRepository.findById(request.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!isMember(group, newOwner)) {
            throw new IllegalArgumentException("New owner must already be a group member");
        }
        group.setOwner(newOwner);
        return group;
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional
    public Group addUsersToGroup(Long groupId, List<Long> userIds) {
        Group group = groupAccessService.requireMember(groupId);
        groupAccessService.requireOwner(group, groupAccessService.currentUser());
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one user id is required");
        }

        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.stream().distinct().count()) {
            throw new NoSuchElementException("One or more users do not exist");
        }
        for (User user : users) {
            if (!isMember(group, user)) {
                group.getMembers().add(user);
            }
        }
        return group;
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        Group group = groupAccessService.requireMember(groupId);
        groupAccessService.requireOwner(group, groupAccessService.currentUser());
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!isMember(group, member)) {
            throw new IllegalArgumentException("User is not a member of this group");
        }
        if (group.getOwner() != null && group.getOwner().getId().equals(member.getId())) {
            throw new IllegalArgumentException("Transfer ownership before removing the group owner");
        }
        if (hasOutstandingBalance(groupId, member)) {
            throw new IllegalArgumentException("Cannot remove a member with an unsettled balance");
        }
        group.getMembers().removeIf(user -> user.getId().equals(member.getId()));
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional
    public GroupInvite createInvite(Long groupId, CreateInviteRequest request) {
        Group group = groupAccessService.requireMember(groupId);
        User currentUser = groupAccessService.currentUser();
        groupAccessService.requireOwner(group, currentUser);

        int expiresInHours = request == null || request.expiresInHours() == null
                ? DEFAULT_INVITE_EXPIRY_HOURS : request.expiresInHours();
        if (expiresInHours < 1 || expiresInHours > MAX_INVITE_EXPIRY_HOURS) {
            throw new IllegalArgumentException("Invite expiry must be between 1 and 720 hours");
        }
        if (request != null && request.maxUses() != null && request.maxUses() < 1) {
            throw new IllegalArgumentException("Invite max uses must be at least 1");
        }

        GroupInvite invite = new GroupInvite();
        invite.setGroup(group);
        invite.setCreatedBy(currentUser);
        invite.setCode(UUID.randomUUID().toString());
        invite.setExpiresAt(Instant.now().plus(expiresInHours, ChronoUnit.HOURS));
        invite.setMaxUses(request == null ? null : request.maxUses());
        invite.setUsedCount(0);
        invite.setRevoked(false);
        return inviteRepository.save(invite);
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional(readOnly = true)
    public List<GroupInvite> getInvites(Long groupId) {
        Group group = groupAccessService.requireMember(groupId);
        groupAccessService.requireOwner(group, groupAccessService.currentUser());
        return inviteRepository.findByGroupId(groupId);
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional
    public void revokeInvite(Long groupId, Long inviteId) {
        Group group = groupAccessService.requireMember(groupId);
        groupAccessService.requireOwner(group, groupAccessService.currentUser());
        GroupInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new NoSuchElementException("Invite not found"));
        if (!invite.getGroup().getId().equals(group.getId())) {
            throw new NoSuchElementException("Invite not found for this group");
        }
        invite.setRevoked(true);
    }

    @Transactional
    public Group joinWithInvite(String code) {
        GroupInvite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Invite not found"));
        if (invite.isRevoked() || !invite.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Invite is expired or revoked");
        }
        if (invite.getMaxUses() != null && invite.getUsedCount() >= invite.getMaxUses()) {
            throw new IllegalArgumentException("Invite has reached its maximum use limit");
        }

        User currentUser = groupAccessService.currentUser();
        Group group = invite.getGroup();
        if (isMember(group, currentUser)) {
            return group;
        }

        group.getMembers().add(currentUser);
        invite.setUsedCount(invite.getUsedCount() + 1);
        return group;
    }

    @Transactional(readOnly = true)
    public List<Group> getAllGroups() {
        return groupRepository.findDistinctByMembersEmail(groupAccessService.currentUser().getEmail());
    }

    @PreAuthorize("@groupAccessService.isOwner(#groupId, authentication.name)")
    @Transactional
    public void deleteGroup(Long groupId) {
        Group group = groupAccessService.requireMember(groupId);
        groupAccessService.requireOwner(group, groupAccessService.currentUser());
        if (expenseRepository.existsByGroupId(groupId)) {
            throw new IllegalArgumentException("Delete all expenses before deleting this group");
        }
        inviteRepository.deleteByGroupId(groupId);
        groupRepository.delete(group);
    }

    private boolean isMember(Group group, User user) {
        return group.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
    }

    private boolean hasOutstandingBalance(Long groupId, User member) {
        double balance = splitRepository.findByExpenseGroupId(groupId).stream()
                .filter(split -> split.getUser().getId().equals(member.getId()))
                .mapToDouble(split -> split.getAmountPaid() - split.getAmountOwed())
                .sum();
        return Math.abs(balance) >= 0.0001;
    }

    private String requiredName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }
        return name.trim();
    }
}
