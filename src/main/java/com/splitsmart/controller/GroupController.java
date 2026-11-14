package com.splitsmart.controller;

import com.splitsmart.dto.CreateGroupRequest;
import com.splitsmart.dto.CreateInviteRequest;
import com.splitsmart.dto.UpdateGroupRequest;
import com.splitsmart.dto.TransferOwnershipRequest;
import com.splitsmart.entity.Group;
import com.splitsmart.entity.GroupInvite;
import com.splitsmart.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Group createGroup(@RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request);
    }

    @GetMapping
    public List<Group> getGroups() { return groupService.getAllGroups(); }

    @GetMapping("/{groupId}")
    public Group getGroup(@PathVariable Long groupId) { return groupService.getGroup(groupId); }

    @PatchMapping("/{groupId}")
    public Group updateGroup(@PathVariable Long groupId, @RequestBody UpdateGroupRequest request) {
        return groupService.updateGroup(groupId, request);
    }

    @PatchMapping("/{groupId}/owner")
    public Group transferOwnership(@PathVariable Long groupId,
                                   @RequestBody TransferOwnershipRequest request) {
        return groupService.transferOwnership(groupId, request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long groupId) { groupService.deleteGroup(groupId); }

    @PostMapping("/{groupId}/users")
    public Group addUsers(@PathVariable Long groupId, @RequestBody List<Long> userIds) {
        return groupService.addUsersToGroup(groupId, userIds);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(groupId, userId);
    }

    @PostMapping("/{groupId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupInvite createInvite(@PathVariable Long groupId, @RequestBody(required = false) CreateInviteRequest request) {
        return groupService.createInvite(groupId, request);
    }

    @GetMapping("/{groupId}/invites")
    public List<GroupInvite> getInvites(@PathVariable Long groupId) { return groupService.getInvites(groupId); }

    @DeleteMapping("/{groupId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvite(@PathVariable Long groupId, @PathVariable Long inviteId) {
        groupService.revokeInvite(groupId, inviteId);
    }

    @PostMapping("/join/{inviteCode}")
    public Group joinWithInvite(@PathVariable String inviteCode) { return groupService.joinWithInvite(inviteCode); }
}
