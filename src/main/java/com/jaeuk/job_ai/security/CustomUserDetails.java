package com.jaeuk.job_ai.security;

import com.jaeuk.job_ai.entity.User;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().getValue())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // email을 username으로 사용
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public User getUser() { return user; }
    public Long getId() { return user.getId(); }
    public String getName() { return user.getName(); }
}