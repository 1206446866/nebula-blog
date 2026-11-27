package com.nebula.auth.service.impl;

import com.nebula.auth.service.LocationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class Ip2RegionLocationServiceImpl implements LocationService {

    private Searcher v4Searcher;

    private Searcher v6Searcher;
    private final List<Path> temporaryDatabases = new ArrayList<>(2);

    @PostConstruct
    public void init() throws IOException {
        try {
            v4Searcher = openSearcher("ip/ip2region_v4.xdb");
            v6Searcher = openSearcher("ip/ip2region_v6.xdb");
        } catch (IOException | RuntimeException e) {
            destroy();
            throw e;
        }
    }

    /** 查询器需要随机读取磁盘文件，打包后的资源先复制到临时文件。 */
    private Searcher openSearcher(String resourcePath) throws IOException {
        var resource = new ClassPathResource(resourcePath);
        if (resource.isFile()) {
            return Searcher.newWithFileOnly(resource.getFile().getAbsolutePath());
        }
        Path file = Files.createTempFile("nebula-ip2region-", ".xdb");
        temporaryDatabases.add(file);
        try (var input = resource.getInputStream()) {
            Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return Searcher.newWithFileOnly(file.toString());
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

    /** 从归属地原始字段中组合城市和国家，缺少城市时仅返回国家。 */
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
        for (Path file : temporaryDatabases) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                file.toFile().deleteOnExit();
            }
        }
        temporaryDatabases.clear();
    }


    /** 安全关闭归属地查询器，忽略关闭阶段的IO异常。 */
    private void close(Searcher searcher) {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (IOException ignored) {
            }
        }
    }
}
