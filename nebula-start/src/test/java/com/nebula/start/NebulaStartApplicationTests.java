package com.nebula.start;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class NebulaStartApplicationTests {


    @Test
    void contextLoads() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String password = "123456"; // 加密
        String encode = passwordEncoder.encode(password);
        System.out.println("加密后密码：");
        System.out.println(encode); // 校验
        boolean matches = passwordEncoder.matches(password, encode);
        System.out.println("是否匹配：");
        System.out.println(matches);
    }

}
