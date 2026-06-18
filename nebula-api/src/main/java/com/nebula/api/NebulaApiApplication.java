package com.nebula.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nebula.api.mapper")
public class NebulaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NebulaApiApplication.class, args);
	}

}
