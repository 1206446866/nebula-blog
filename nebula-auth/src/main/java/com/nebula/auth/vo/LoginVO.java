package com.nebula.auth.vo;

import com.nebula.user.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 登录响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class LoginVO {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 当前登录用户
     */
    private UserVO user;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    public static LoginVO create(){
        return new LoginVO();
    }
}