package com.nebula.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

/** Forwarding headers are meaningful only behind explicitly trusted proxy peers. */
@Component
public class ClientIpResolver {
    private final List<IpAddressMatcher> proxies;

    public ClientIpResolver(@Value("${nebula.security.trusted-proxies:}") List<String> trustedProxies) {
        proxies = trustedProxies.stream().map(String::strip).filter(value -> !value.isEmpty())
                .map(IpAddressMatcher::new).toList();
    }

    public String getIp(HttpServletRequest request) {
        String remote = RequestUtils.getIp(request);
        if (!trusted(remote)) return remote;
        var headers = request.getHeaders("X-Forwarded-For");
        if (headers == null || !headers.hasMoreElements()) return remote;
        String forwarded = headers.nextElement();
        if (headers.hasMoreElements() || forwarded == null || forwarded.length() > 2048) return remote;
        String[] chain = forwarded.split(",", -1);
        if (chain.length > 20) return remote;
        String[] addresses = new String[chain.length];
        for (int i = 0; i < chain.length; i++) {
            addresses[i] = RequestUtils.normalizeIp(chain[i]);
            if (addresses[i] == null) return remote;
        }
        String current = remote;
        for (int i = addresses.length - 1; i >= 0 && trusted(current); i--) current = addresses[i];
        return current;
    }

    private boolean trusted(String ip) {
        return !"Unknown".equals(ip) && proxies.stream().anyMatch(proxy -> proxy.matches(ip));
    }
}
