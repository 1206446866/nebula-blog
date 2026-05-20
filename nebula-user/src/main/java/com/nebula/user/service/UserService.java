package com.nebula.user.service;

import com.mybatisflex.core.service.IService;
import com.nebula.user.entity.User;

import java.util.List;


public interface UserService extends IService<User> {

    List<User> getUsers(String role, String username, int page, int size);

    long countUsers(String role, String username);
}