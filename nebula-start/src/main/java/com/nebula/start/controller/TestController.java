package com.nebula.start.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    UserMapper userMapper;

    @GetMapping
    public String test() {
        System.out.println(userMapper.selectOneByQuery(QueryWrapper.create().eq("username", "admin")));
        return "ok";
    }
}