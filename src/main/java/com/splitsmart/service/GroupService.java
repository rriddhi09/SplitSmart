package com.splitsmart.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.splitsmart.entity.Group;
import com.splitsmart.entity.User;
import com.splitsmart.repository.GroupRepository;
import com.splitsmart.repository.UserRepository;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    // Create Group
    public Group createGroup(Group group) {
        return groupRepository.save(group);
    }

    // Add users to group
    public Group addUsersToGroup(Long groupId, List<Long> userIds) {

        Group group = groupRepository.findById(groupId).orElseThrow();

        List<User> users = userRepository.findAllById(userIds);

        group.setMembers(users);

        return groupRepository.save(group);
    }

    // Get all groups
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }
}