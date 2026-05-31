package com.nebula.start.controller;

import com.nebula.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("hello");
    }

    @PreAuthorize("hasAuthority('user:create')")
    @GetMapping("/user/create")
    public Result<String> list() {
        return Result.success("user create");
    }
}