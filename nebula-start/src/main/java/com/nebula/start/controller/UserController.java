package com.nebula.start.controller;

import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping
    public List<User> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getUsers(role, username, page, size);
    }

    @GetMapping("/count")
    public long countUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String username) {
        return userService.countUsers(role, username);
    }
}