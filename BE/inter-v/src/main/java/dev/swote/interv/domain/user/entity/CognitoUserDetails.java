package dev.swote.interv.domain.user.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CognitoUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final User user;

    public CognitoUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 기본적으로 모든 사용자에게 USER 역할 부여
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        // Cognito에서 인증을 처리하므로 비밀번호는 null
        return null;
    }

    @Override
    public String getUsername() {
        // 이메일을 기본 사용자 이름으로 사용
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getDeletedAt() == null;
    }

    // User 객체 접근 메서드
    public User getUser() {
        return user;
    }

    // 사용자 ID 접근 메서드
    public Integer getUserId() {
        return user.getId();
    }
}