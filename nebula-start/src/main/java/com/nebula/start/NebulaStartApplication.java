package com.nebula.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@MapperScan("com.nebula.start.mapper")
@SpringBootApplication
public class NebulaStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaStartApplication.class, args);
    }

}
