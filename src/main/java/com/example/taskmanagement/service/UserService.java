package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.UserCreateRequest;
import com.example.taskmanagement.dto.UserResponse;
import com.example.taskmanagement.dto.UserUpdateRequest;
import com.example.taskmanagement.exception.DuplicateResourceException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.model.Role;
import com.example.taskmanagement.model.User;
import com.example.taskmanagement.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(String id) {
        return UserResponse.from(getUserOrThrow(id));
    }

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        Set<Role> roles = (request.roles() == null || request.roles().isEmpty())
                ? EnumSet.of(Role.USER)
                : EnumSet.copyOf(request.roles());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(roles)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse update(String id, UserUpdateRequest request) {
        User user = getUserOrThrow(id);

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Email already registered: " + request.email());
            }
            user.setEmail(request.email());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(EnumSet.copyOf(request.roles()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    public void delete(String id) {
        User user = getUserOrThrow(id);
        userRepository.delete(user);
    }

    private User getUserOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
