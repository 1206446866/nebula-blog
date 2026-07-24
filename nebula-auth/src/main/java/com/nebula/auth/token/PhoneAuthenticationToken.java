package com.nebula.auth.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class PhoneAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private Object credentials;


    /**
     * 未认证状态
     */
    public PhoneAuthenticationToken(String phone, String password) {
        super(null);
        this.principal = phone;
        this.credentials = password;
        setAuthenticated(false);
    }


    /**
     * 认证成功状态
     */
    public PhoneAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }


    @Override
    public Object getCredentials() {
        return credentials;
    }


    @Override
    public Object getPrincipal() {
        return principal;
    }
}
