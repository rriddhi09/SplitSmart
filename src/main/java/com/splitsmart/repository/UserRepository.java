package com.splitsmart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.splitsmart.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // custom method (very useful later)
    User findByEmail(String email);
}