package com.nebula.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nebula.auth.mapper")
public class NebulaAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaAuthApplication.class, args);
    }

}
