package com.nebula.user.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> list(@RequestParam(required = false) String role) {
        return userService.getUsersByRole(role);
    }

    @GetMapping("/page")
    public Page<User> page(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String username) {
        return userService.pageUsers(page, size, role, username);
    }

    @PostMapping
    public boolean add(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping
    public boolean update(@RequestBody User user) {
        return userService.updateById(user);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return userService.removeById(id);
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.getById(id);
    }
}