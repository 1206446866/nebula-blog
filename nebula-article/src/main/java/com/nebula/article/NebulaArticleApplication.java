package com.nebula.article;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com/nebula/article/mapper")
public class NebulaArticleApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaArticleApplication.class, args);
    }

}
