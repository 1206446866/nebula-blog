package com.nebula.role;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nebula.role.mapper")
public class NebulaRoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaRoleApplication.class, args);
    }

}
