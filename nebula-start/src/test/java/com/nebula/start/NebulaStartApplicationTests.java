package com.nebula.start;

import com.nebula.user.entity.User;
import com.nebula.start.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

@SpringBootTest
class NebulaStartApplicationTests {

@Autowired
UserMapper userMapper;

    @Test
    void contextLoads() {
//        System.out.println(userMapper.selectAll());
        System.out.println(new User().list());
    }

}
