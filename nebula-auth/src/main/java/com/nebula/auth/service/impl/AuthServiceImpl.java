package com.nebula.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.auth.dto.*;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.service.LoginLogService;
import com.nebula.auth.token.EmailAuthenticationToken;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.vo.LoginVO;
import com.nebula.common.constant.LoginStatus;
import com.nebula.common.constant.RoleEnum;
import com.nebula.common.constant.redisKey.AuthRedisKey;
import com.nebula.common.exception.AuthException;
import com.nebula.common.mail.service.MailService;
import com.nebula.common.redis.RedisService;
import com.nebula.common.security.LoginUser;
import com.nebula.common.util.SecurityUtils;
import com.nebula.role.entity.Permission;
import com.nebula.role.entity.RolePermission;
import com.nebula.role.mapper.PermissionMapper;
import com.nebula.role.mapper.RolePermissionMapper;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import com.nebula.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.nebula.role.entity.table.PermissionTableDef.PERMISSION;
import static com.nebula.role.entity.table.RolePermissionTableDef.ROLE_PERMISSION;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.user.entity.table.UserTableDef.USER;

/**
 * 认证 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * 用户 User
     */
    private final UserMapper userMapper;

    /**
     * 用户角色关联 UserRole
     */
    private final UserRoleMapper userRoleMapper;


    /**
     * 角色权限关联 RolePermissionMapper
     */
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 权限 PermissionMapper
     */
    private final PermissionMapper permissionMapper;

    private final LoginLogService loginLogService;

    private final AuthenticationManager authenticationManager;
    /**
     * JWT 工具类
     */
    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final RedisService redisService;

    private final MailService mailService;


    @Override
    public Boolean changePassword(ChangePasswordDTO dto) {
        LoginUser lu = SecurityUtils.getLoginUser();
        if (Objects.isNull(lu) || Objects.isNull(lu.getPassword())) {
            throw new RuntimeException("你还没有登录");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), lu.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次密码不一致");
        }
        User user = User.create();
        user.setId(lu.getUserId());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        return userMapper.update(user) > 0;
    }

    //TODO
    @Override
    public Boolean resetPassword(Long userId) {
        return false;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        Authentication authentication;
        try {
            if (loginDTO.getAccount().contains("@")) {
                authentication = authenticationManager.authenticate(new EmailAuthenticationToken(loginDTO.getAccount(), loginDTO.getPassword()));
            } else {
                authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getAccount(), loginDTO.getPassword()));
            }
        } catch (AuthenticationException e) {
            Integer loginStatus = getLoginStatus(e);
            //保证登录记录一定是已存账号，不存在的账号不记录
            User user = findUserByAccount(loginDTO.getAccount());
            if (Objects.nonNull(user)) loginLogService.recordLoginLog(user.getId(), loginStatus);
            throw new AuthException(LoginStatus.fromCode(loginStatus));
        }
        AuthLoginUser loginUser = (AuthLoginUser) authentication.getPrincipal();
        loginLogService.recordLoginLog(loginUser.getUserId(), LoginStatus.SUCCESS.getCode());
        User user = loginUser.getUser();
        String token = jwtUtil.createToken(user);
        return LoginVO.create().setToken(token).setUser(BeanUtil.copyProperties(user, UserVO.class)).setRoles(loginUser.getRoles()).setPermissions(loginUser.getPermissions());
    }


    @Override
    public Long sendEmailCaptcha(String email) {
        String cdKey = AuthRedisKey.EMAIL_CAPTCHA_CD + email;

        if (redisService.hasKey(cdKey)) {
            return redisService.getExpire(cdKey);
        }

        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));

        redisService.set(AuthRedisKey.EMAIL_CAPTCHA + email, code, 5, TimeUnit.MINUTES);

        redisService.set(cdKey, "1", 60, TimeUnit.SECONDS);

        mailService.sendText(email, "Nebula Blog 注册验证码", "您的验证码为：" + code + "\n5分钟内有效，请勿泄露。");
        return redisService.getExpire(cdKey);
    }

    @Override
    public String verifyEmailCaptcha(EmailVerifyDTO dto) {
        String email = dto.getEmail();
        String key = AuthRedisKey.EMAIL_CAPTCHA + email;

        Object captcha = redisService.get(key);

        if (Objects.isNull(captcha)) {
            throw new RuntimeException("验证码已过期");
        }

        if (!dto.getCode().equals(captcha.toString())) {
            throw new RuntimeException("验证码错误");
        }

        if (userMapper.selectCountByQuery(QueryWrapper.create().where(USER.EMAIL.eq(dto.getEmail()))) > 0) {
            throw new RuntimeException("该邮箱已注册");
        }
        // 验证码一次性使用
        redisService.delete(key);

        String verifyToken = UUID.randomUUID().toString().replace("-", "");

        RegisterCacheDTO cacheDTO = new RegisterCacheDTO();
        cacheDTO.setAccount(email);
        cacheDTO.setPassword(passwordEncoder.encode(dto.getPassword()));
        redisService.set(AuthRedisKey.REGISTER_TOKEN + verifyToken, cacheDTO, 10, TimeUnit.MINUTES);


        // 发送确认邮件
        mailService.sendHtml(email, "Nebula Blog 注册确认", """
                <h3>Nebula Blog 注册确认</h3>
                <p>您正在注册 Nebula Blog 账号。</p>
                <p>请点击下面链接完成注册：</p>
                <a href="http://localhost:5173/register?token=%s">
                完成注册
                </a>
                <p>该链接10分钟内有效。</p>
                """.formatted(verifyToken));
        return "验证成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean register(RegisterDTO dto) {
        String key = AuthRedisKey.REGISTER_TOKEN + dto.getToken();

        RegisterCacheDTO cache = redisService.get(key);

        if (cache == null) {
            throw new RuntimeException("注册链接已失效");
        }
//        用户创建
        User user = User.create()
                .setNid("registering..." + UUID.randomUUID())
                .setUsername("registering..." + UUID.randomUUID())
                .setPassword(cache.getPassword());
        userMapper.insertSelective(user);
        user.setNid(String.valueOf(user.getId())).setUsername("普通用户" + user.getId());
        userMapper.update(user);
        UserRole userRole = UserRole.create().setUserId(user.getId()).setRoleId(RoleEnum.USER.getRoleId());
//        角色关联
        Boolean result = userRoleMapper.insertSelective(userRole) > 0;

        if (result) {
            redisService.delete(key);
        }

        return result;
    }

    @Override
    public List<String> getUserPermissionsByUserId(Long userId) {
        // 1. 查询用户角色
        List<Long> roleIds = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(userId)).stream().map(UserRole::getRoleId).collect(Collectors.toList());

        if (roleIds.isEmpty()) return List.of();

        // 2. 查询角色权限
        List<Long> permissionIds = rolePermissionMapper.selectListByCondition(ROLE_PERMISSION.ROLE_ID.in(roleIds)).stream().map(RolePermission::getPermissionId).collect(Collectors.toList());

        if (permissionIds.isEmpty()) return List.of();

        // 3. 查询权限标识
        return permissionMapper.selectListByCondition(PERMISSION.ID.in(permissionIds)).stream().map(Permission::getName).collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        List<String> permissions = getUserPermissionsByUserId(userId);
        return permissions.contains(permission);
    }

    private Integer getLoginStatus(Exception e) {
        if (e instanceof UsernameNotFoundException) {
            return LoginStatus.USER_NOT_FOUND.getCode();
        }

        if (e instanceof BadCredentialsException) {
            return LoginStatus.PASSWORD_ERROR.getCode();
        }

        if (e instanceof DisabledException) {
            return LoginStatus.USER_DISABLED.getCode();
        }

        if (e instanceof LockedException) {
            return LoginStatus.USER_LOCKED.getCode();
        }
        return LoginStatus.FAIL.getCode();
    }

    private User findUserByAccount(String account) {

        if (account.contains("@")) {
            return userMapper.selectOneByCondition(
                    USER.EMAIL.eq(account)
            );
        }

        return userMapper.selectOneByCondition(
                USER.NID.eq(account)
        );
    }

}
