package com.nebula.auth;

import com.nebula.auth.provider.EmailAuthenticationProvider;
import com.nebula.auth.provider.PhoneAuthenticationProvider;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.token.EmailAuthenticationToken;
import com.nebula.auth.token.PhoneAuthenticationToken;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordProviderAccountStateTest {
    private final UserMapper users = mock(UserMapper.class);
    private final AuthUserBuilder builder = mock(AuthUserBuilder.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);

    private Authentication authenticate(String channel, User user, String password) {
        when(users.selectOneByCondition(any())).thenReturn(user);
        if (user != null) {
            when(builder.createLoginUser(user)).thenReturn(new AuthLoginUser(user, List.of(), List.of()));
            when(passwords.matches("correct", "stored-hash")).thenReturn(true);
        }
        AuthenticationProvider provider = channel.equals("email")
                ? new EmailAuthenticationProvider(users, builder, passwords)
                : new PhoneAuthenticationProvider(users, builder, passwords);
        Authentication token = channel.equals("email")
                ? new EmailAuthenticationToken("reader@example.invalid", password)
                : new PhoneAuthenticationToken("13900000000", password);
        return provider.authenticate(token);
    }

    private User account(Integer status, int deleted) {
        return User.create().setId(42L).setPassword("stored-hash").setStatus(status).setDeleted(deleted);
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "phone"})
    void enabledAccountsCanAuthenticate(String channel) {
        var result = authenticate(channel, account(0, 0), "correct");
        assertTrue(result.isAuthenticated());
        assertEquals(42L, ((AuthLoginUser) result.getPrincipal()).getUserId());
        assertNull(result.getCredentials());
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "phone"})
    void disabledAccountsAreRejected(String channel) {
        assertThrows(DisabledException.class, () -> authenticate(channel, account(1, 0), "correct"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "phone"})
    void lockedAccountsAreRejectedAsLocked(String channel) {
        assertThrows(LockedException.class, () -> authenticate(channel, account(2, 0), "correct"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "phone"})
    void deletedAccountsAreRejectedEvenIfAMapperReturnsThem(String channel) {
        assertThrows(DisabledException.class, () -> authenticate(channel, account(0, 1), "correct"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "phone"})
    void unknownAndMissingAccountStatesFailClosed(String channel) {
        assertThrows(DisabledException.class, () -> authenticate(channel, account(99, 0), "correct"));
        assertThrows(DisabledException.class, () -> authenticate(channel, account(null, 0), "correct"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "phone"})
    void wrongPasswordsAndMissingUsersCannotAuthenticate(String channel) {
        assertThrows(BadCredentialsException.class, () -> authenticate(channel, account(0, 0), "wrong"));
        assertThrows(BadCredentialsException.class, () -> authenticate(channel, null, "correct"));
    }
}
