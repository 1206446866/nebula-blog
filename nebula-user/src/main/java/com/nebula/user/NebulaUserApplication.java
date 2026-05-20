package com.nebula.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nebula.user.mapper")
public class NebulaUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaUserApplication.class, args);
    }

}
