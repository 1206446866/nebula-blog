package com.nebula.auth.service.impl;

import com.nebula.auth.service.LocationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class Ip2RegionLocationServiceImpl implements LocationService {

    private Searcher v4Searcher;

    private Searcher v6Searcher;

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource v4 = new ClassPathResource("ip/ip2region_v4.xdb");
        ClassPathResource v6 = new ClassPathResource("ip/ip2region_v6.xdb");

        v4Searcher = Searcher.newWithFileOnly(
                v4.getFile().getAbsolutePath()
        );

        v6Searcher = Searcher.newWithFileOnly(
                v6.getFile().getAbsolutePath()
        );
    }


    @Override
    public String getLocation(String ip) {

        if (ip == null || ip.isEmpty()) {
            return "Unknown";
        }

        // 本地开发
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip) || "::1".equals(ip)  || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "Local";
        }

        try {

            String region;

            if (isIPv4(ip)) {
                region = v4Searcher.search(ip);
            } else {
                region = v6Searcher.search(ip);
            }

            if (region == null || region.isEmpty()) {
                return "Unknown";
            }

            return format(region);

        } catch (Exception e) {

            return "Unknown";
        }
    }

    /**
     * 判断IPv4
     */
    private boolean isIPv4(String ip) {
        return ip.contains(".");
    }

    private String format(String region) {

        /*
         * ip2region返回：
         *
         * 国家|区域|省份|城市|ISP
         *
         */

        String[] arr = region.split("\\|");

        if (arr.length >= 4) {

            String country = arr[0];
            String city = arr[3];

            if (!city.equals("0") && !city.isEmpty()) {
                return city + ", " + country;
            }

            return country;
        }

        return region;
    }


    @PreDestroy
    public void destroy() {
        close(v4Searcher);
        close(v6Searcher);
    }


    private void close(Searcher searcher) {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (IOException ignored) {
            }
        }
    }
}
