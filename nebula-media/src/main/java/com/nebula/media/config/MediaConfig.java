package com.nebula.media.config;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
@Configuration
@MapperScan("com.nebula.media.mapper")
public class MediaConfig {}
