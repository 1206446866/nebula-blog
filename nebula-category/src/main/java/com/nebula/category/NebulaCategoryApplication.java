package com.nebula.category;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com/nebula/categor y/mapper")
public class NebulaCategoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaCategoryApplication.class, args);
    }

}
