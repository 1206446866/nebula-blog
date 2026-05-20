package com.nebula.comment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nebula.comment.mapper")
public class NebulaCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(NebulaCommentApplication.class, args);
    }

}
