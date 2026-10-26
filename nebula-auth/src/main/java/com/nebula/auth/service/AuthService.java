package com.nebula.auth.service;

import com.nebula.auth.dto.ChangePasswordDTO;
import com.nebula.auth.dto.EmailVerifyDTO;
import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.dto.RegisterDTO;
import com.nebula.auth.vo.LoginVO;

import java.util.List;

/** 登录、会话、邮箱注册与密码管理服务。 */
public interface AuthService {


    /**
     * 校验账号、密码和账号状态，登记会话并返回登录信息。
     *
     * @param loginDTO 账号与密码
     * @return Token、用户公开资料、角色和权限
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 返回当前有效会话的用户、角色和权限，不返回密码或Token。
     *
     * @return 当前用户的会话资料
     */
    com.nebula.auth.vo.CurrentSessionVO currentSession();

    /**
     * 撤销本次请求使用的登录会话，不影响其他会话。
     *
     * @param token 当前请求的Bearer Token
     */
    void logout(String token);

    /**
     * 校验发送限额后向注册邮箱发送验证码。
     *
     * @param email 注册邮箱
     * @return 再次发送前需等待的秒数
     */
    Long sendEmailCaptcha(String email);

    /**
     * 校验6至64字符且不超过72字节的新密码及邮箱验证码，再发送一次性注册确认链接。
     *
     * @param dto 邮箱、验证码与注册密码
     * @return 验证结果提示
     */
    String verifyEmailCaptcha(EmailVerifyDTO dto);

    /**
     * 消费注册凭证，在同一事务中创建用户并绑定普通用户角色。
     *
     * @param dto 一次性注册凭证
     * @return 是否注册成功
     */
    Boolean register(RegisterDTO dto);

    /**
     * 使用共享失败预算校验当前密码后修改本人密码，使原有登录会话失效；仅新密码受长度规则约束。
     *
     * @param dto 原密码、新密码与确认密码，不可为空
     * @return 是否修改成功
     */
    Boolean changePassword(ChangePasswordDTO dto);

    /**
     * 管理员使用共享失败预算验证本人密码后重置目标密码，并保护更高权限账号；新密码适用统一规则。
     *
     * @param userId 目标用户ID
     * @param dto 管理员当前密码及目标新密码，不可为空；操作者必须具有有效登录主体
     * @return 是否重置成功
     */
    Boolean resetPassword(Long userId, com.nebula.auth.dto.ResetPasswordDTO dto);

    /**
     * 查询用户当前有效的权限标识，包含超级管理员的固有权限。
     *
     * @param userId 用户ID
     * @return 权限标识列表，例如article:list
     */
    List<String> getUserPermissionsByUserId(Long userId);

    /**
     * 判断指定用户当前是否拥有某项权限。
     *
     * @param userId 用户ID
     * @param permission 完整权限标识
     * @return 是否拥有该权限
     */
    boolean hasPermission(Long userId, String permission);


}
