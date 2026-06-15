package com.nebula.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class EditUserDTO {
    private Long id;
    private String username;
    private List<Long> roleIds;
}