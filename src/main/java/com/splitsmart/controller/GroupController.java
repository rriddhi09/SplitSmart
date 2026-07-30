package com.splitsmart.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.splitsmart.entity.Group;
import com.splitsmart.service.GroupService;

@RestController
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    // Create group
    @PostMapping
    public Group createGroup(@RequestBody Group group) {
        return groupService.createGroup(group);
    }

    // Add users to group
    @PostMapping("/{groupId}/users")
    public Group addUsers(
            @PathVariable Long groupId,
            @RequestBody List<Long> userIds) {

        return groupService.addUsersToGroup(groupId, userIds);
    }

    // Get all groups
    @GetMapping
    public List<Group> getGroups() {
        return groupService.getAllGroups();
    }
}