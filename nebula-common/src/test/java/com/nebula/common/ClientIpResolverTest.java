package com.nebula.common;

import com.nebula.common.util.ClientIpResolver;
import com.nebula.common.util.RequestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {
    private MockHttpServletRequest request(String remote, String forwarded) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(remote);
        if (forwarded != null) request.addHeader("X-Forwarded-For", forwarded);
        request.addHeader("Proxy-Client-IP", "1.2.3.4");
        request.addHeader("Forwarded", "for=1.2.3.4");
        return request;
    }

    @Test
    void directClientsAndLoopbackAreNotImplicitlyTrusted() {
        var resolver = new ClientIpResolver(List.of());
        for (String remote : List.of("127.0.0.1", "198.51.100.10")) {
            var request = request(remote, "1.2.3.4");
            assertEquals(remote, resolver.getIp(request));
            assertEquals(remote, RequestUtils.getIp(request));
        }
    }

    @Test
    void proxyChainsAreWalkedFromTheTrustedPeerToTheFirstUntrustedHop() {
        var resolver = new ClientIpResolver(List.of("10.0.0.0/8"));
        assertEquals("198.51.100.30", resolver.getIp(request("10.0.0.1", "1.2.3.4, 198.51.100.30, 10.0.0.2")));
        assertEquals("198.51.100.30", resolver.getIp(request("10.0.0.1", "198.51.100.30")));
        assertEquals("198.51.100.10", resolver.getIp(request("198.51.100.10", "1.2.3.4, 10.0.0.2")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.invalid", "198.51.100.2,", "unknown", "198.51.100.2:8080", "999.2.3.4", "[2001:db8::1]:8080"})
    void malformedOrHostnameHeadersFallBackToThePeerWithoutDns(String forwarded) {
        assertEquals("10.0.0.1", new ClientIpResolver(List.of("10.0.0.0/8")).getIp(request("10.0.0.1", forwarded)));
    }

    @Test
    void duplicateAndOversizedHeadersAreRejected() {
        var resolver = new ClientIpResolver(List.of("10.0.0.0/8"));
        var duplicate = request("10.0.0.1", "198.51.100.20");
        duplicate.addHeader("X-Forwarded-For", "198.51.100.21");
        assertEquals("10.0.0.1", resolver.getIp(duplicate));
        assertEquals("10.0.0.1", resolver.getIp(request("10.0.0.1", "198.51.100.20,".repeat(200))));
    }

    @Test
    void ipv6NetworksAndEquivalentAddressesAreCanonicalized() {
        var resolver = new ClientIpResolver(List.of("2001:db8:1::/48"));
        assertEquals("2001:db8:2:0:0:0:0:3", resolver.getIp(request("2001:db8:1::1", "2001:db8:2::3")));
        assertEquals("127.0.0.1", new ClientIpResolver(List.of()).getIp(request("::ffff:127.0.0.1", "1.2.3.4")));
    }
}
