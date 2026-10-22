package com.nebula.auth;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.nebula.auth.entity.LoginLog;
import com.nebula.auth.service.LocationService;
import com.nebula.auth.service.impl.LoginLogServiceImpl;
import com.nebula.common.util.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoginLogMetadataTest {
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final LoginLogServiceImpl service = spy(new LoginLogServiceImpl(request, mock(LocationService.class), new ClientIpResolver(List.of())));

    private LoginLog record() {
        List<LoginLog> saved = new ArrayList<>();
        doAnswer(invocation -> { saved.add(invocation.getArgument(0)); return true; })
                .when(service).save(any(LoginLog.class));
        service.recordLoginLog(42L, 1);
        assertEquals(1, saved.size(), "Metadata fallback must still save the audit record");
        return saved.get(0);
    }

    private void assertUnknown(LoginLog log) {
        assertEquals("Unknown", log.getBrowser());
        assertEquals("Unknown", log.getOs());
        assertEquals("Unknown", log.getDevice());
    }

    @Test
    void missingMetadataUsesSafeDefaults() {
        var log = record();
        assertEquals("Unknown", log.getUserAgent());
        assertUnknown(log);
    }

    @Test
    void parserExceptionsCannotPreventAuditLogging() {
        request.addHeader("User-Agent", "broken-client");
        try (var parser = mockStatic(UserAgentUtil.class)) {
            parser.when(() -> UserAgentUtil.parse("broken-client")).thenThrow(new IllegalArgumentException("bad metadata"));
            var log = record();
            assertEquals("broken-client", log.getUserAgent());
            assertUnknown(log);
        }
    }

    @Test
    void partialParserResultsUseSafeDefaults() {
        request.addHeader("User-Agent", "partial-client");
        try (var parser = mockStatic(UserAgentUtil.class)) {
            parser.when(() -> UserAgentUtil.parse("partial-client")).thenReturn(new UserAgent());
            assertUnknown(record());
        }
    }

    @Test
    void nullParserResultsUseSafeDefaults() {
        request.addHeader("User-Agent", "unknown-client");
        try (var parser = mockStatic(UserAgentUtil.class)) {
            parser.when(() -> UserAgentUtil.parse("unknown-client")).thenReturn(null);
            assertUnknown(record());
        }
    }

    @Test
    void controlCharactersAndSupplementaryUnicodeAreBoundedWithoutSplittingCharacters() {
        request.addHeader("User-Agent", "\r\n\t" + "\uD83C\uDF0C".repeat(600));
        String stored = record().getUserAgent();
        assertTrue(stored.codePointCount(0, stored.length()) <= 512);
        assertFalse(stored.codePoints().anyMatch(Character::isISOControl));
        assertTrue(Character.isLowSurrogate(stored.charAt(stored.length() - 1)));
    }
}
