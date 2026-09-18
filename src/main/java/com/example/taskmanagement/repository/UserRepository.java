package com.example.taskmanagement.repository;

import com.example.taskmanagement.model.Role;
import com.example.taskmanagement.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRolesContaining(Role role);
}
