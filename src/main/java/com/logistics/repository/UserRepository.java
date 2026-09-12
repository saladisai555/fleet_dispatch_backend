package com.logistics.repository;

import com.logistics.entity.User;
import com.logistics.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    // Core need: Spring Security authentication looks users up by email
    Optional<User> findByEmail(String email);

    // Needed to check uniqueness before registration/creation
    boolean existsByEmail(String email);

    // Needed for role-based views (e.g. list all FLEET_MANAGER users to assign as reviewer)
    List<User> findByRole(UserRole role);
}