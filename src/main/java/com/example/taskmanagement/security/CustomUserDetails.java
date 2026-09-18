package com.example.taskmanagement.security;

import com.example.taskmanagement.model.Role;
import com.example.taskmanagement.model.User;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final String id;
    private final String username;
    private final String password;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(String id, String username, String password, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword(), user.getRoles());
    }

    public String getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
