package com.nebula.tag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com/nebula/tag/mapper")
public class NebulaTagApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaTagApplication.class, args);
    }

}
