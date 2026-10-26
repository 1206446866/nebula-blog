package com.nebula.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.auth.dto.*;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.service.LoginLogService;
import com.nebula.auth.service.RegistrationTokenService;
import com.nebula.auth.service.AuthAbuseProtection;
import com.nebula.auth.security.RegistrationUnavailableException;
import com.nebula.auth.util.RegistrationEmail;
import com.nebula.auth.token.EmailAuthenticationToken;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.vo.LoginVO;
import com.nebula.common.constant.LoginStatus;
import com.nebula.common.constant.RoleEnum;
import com.nebula.common.constant.UserStatus;
import com.nebula.common.exception.AuthException;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.mail.service.MailService;
import com.nebula.common.security.LoginUser;
import com.nebula.common.util.SecurityUtils;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.Locale;

import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.user.entity.table.UserTableDef.USER;
import static com.nebula.common.exception.code.RegistrationErrorCode.EMAIL_ALREADY_REGISTERED;
import static com.nebula.common.exception.code.AuthErrorCode.*;
import static com.nebula.common.exception.code.UserErrorCode.USER_SELF_OPERATION_DENIED;

/**
 * 认证 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 用户 User
     */
    private final UserMapper userMapper;

    /**
     * 用户角色关联 UserRole
     */
    private final UserRoleMapper userRoleMapper;


    private final LoginLogService loginLogService;

    private final AuthenticationManager authenticationManager;
    /**
     * JWT 工具类
     */
    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;
    private final PermissionService permissionService;
    private final RegistrationTokenService registrationTokens;
    private final AuthAbuseProtection abuseProtection;
    private final RoleService roleService;
    private final com.nebula.common.security.SensitivePasswordVerifier sensitivePasswords;
    private final com.nebula.auth.security.RegistrationProperties registration;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changePassword(ChangePasswordDTO dto) {
        LoginUser lu = SecurityUtils.getLoginUser();
        if (Objects.isNull(lu) || Objects.isNull(lu.getPassword())) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        if (dto == null) throw new BusinessException(PASSWORD_POLICY_INVALID);
        com.nebula.common.util.PasswordPolicy.validateNewPassword(dto.getNewPassword());
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(PASSWORD_CONFIRMATION_MISMATCH);
        }
        if (!sensitivePasswords.matches(dto.getOldPassword(), lu.getPassword())) throw new BusinessException(OLD_PASSWORD_INCORRECT);
        User user = User.create();
        user.setId(lu.getUserId());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        // 同时匹配认证时的密码哈希，已通过认证的并发请求也不能重复修改旧密码。
        int affected = userMapper.updateByQuery(user, QueryWrapper.create()
                .where(USER.ID.eq(lu.getUserId()))
                .and(USER.PASSWORD.eq(lu.getPassword()))
                .and(USER.STATUS.eq(UserStatus.NORMAL.getCode())));
        if (affected != 1) throw new BusinessException(PASSWORD_CHANGE_CONFLICT);
        return true;
    }

    @Override
    public void logout(String token) {
        if (!Objects.equals(jwtUtil.getUserId(token), SecurityUtils.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("只能退出当前会话");
        }
        jwtUtil.revokeToken(token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resetPassword(Long userId, ResetPasswordDTO dto) {
        LoginUser operator = SecurityUtils.getLoginUser();
        if (operator == null || !SecurityUtils.isAdmin() || !SecurityUtils.hasPermission("user:update")) {
            throw new org.springframework.security.access.AccessDeniedException("仅有用户管理权限的管理员可重置密码");
        }
        if (userId == null || userId < 1) throw new BusinessException(com.nebula.common.exception.code.UserErrorCode.USER_NOT_FOUND);
        if (Objects.equals(userId, operator.getUserId())) throw new BusinessException(USER_SELF_OPERATION_DENIED);
        if (dto == null) throw new BusinessException(PASSWORD_POLICY_INVALID);
        com.nebula.common.util.PasswordPolicy.validateNewPassword(dto.getNewPassword());
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) throw new BusinessException(PASSWORD_CONFIRMATION_MISMATCH);
        if (!sensitivePasswords.matches(dto.getOperatorPassword(), operator.getPassword())) {
            throw new BusinessException(OPERATOR_PASSWORD_INCORRECT);
        }
        // 先锁角色绑定再锁目标账号，避免并发提权绕过角色层级检查。
        var bindings = userRoleMapper.selectListByQuery(QueryWrapper.create().where(USER_ROLE.USER_ID.eq(userId)).forUpdate());
        boolean protectedTarget = bindings.stream().anyMatch(binding ->
                Objects.equals(binding.getRoleId(), RoleEnum.SUPER_ADMIN.getRoleId())
                        || Objects.equals(binding.getRoleId(), RoleEnum.ADMIN.getRoleId()))
                || roleService.getRolesByUserId(userId).stream().map(role -> role.getName())
                .anyMatch(name -> List.of("ADMIN", "ROLE_ADMIN", "SUPER_ADMIN", "ROLE_SUPER_ADMIN").contains(name));
        if (!SecurityUtils.isSuperAdmin() && protectedTarget) {
            throw new org.springframework.security.access.AccessDeniedException("不能重置同级或更高权限账号的密码");
        }
        User target = userMapper.selectOneByQuery(QueryWrapper.create().where(USER.ID.eq(userId)).forUpdate());
        if (target == null) throw new BusinessException(com.nebula.common.exception.code.UserErrorCode.USER_NOT_FOUND);
        int changed = userMapper.updateByQuery(User.create().setPassword(passwordEncoder.encode(dto.getNewPassword())),
                QueryWrapper.create().where(USER.ID.eq(userId)).and(USER.PASSWORD.eq(target.getPassword())));
        if (changed != 1) throw new BusinessException(PASSWORD_CHANGE_CONFLICT);
        return true;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        loginDTO.setAccount(loginDTO.getAccount().strip());
        User account = findUserByAccount(loginDTO.getAccount());
        String subject = account == null ? "account:" + loginDTO.getAccount() : "user:" + account.getId();
        abuseProtection.requireCredentialAttempt("login", subject);
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
            if (Objects.nonNull(account)) loginLogService.recordLoginLog(account.getId(), loginStatus);
            throw new AuthException(LoginStatus.fromCode(loginStatus));
        }
        AuthLoginUser loginUser = (AuthLoginUser) authentication.getPrincipal();
        abuseProtection.credentialSucceeded("login", subject);
        loginLogService.recordLoginLog(loginUser.getUserId(), LoginStatus.SUCCESS.getCode());
        User user = loginUser.getUser();
        String token = jwtUtil.createToken(user);
        return LoginVO.create().setToken(token).setUser(BeanUtil.copyProperties(user, UserVO.class)).setRoles(loginUser.getRoles()).setPermissions(loginUser.getPermissions());
    }


    @Override
    public com.nebula.auth.vo.CurrentSessionVO currentSession() {
        if (!(SecurityUtils.getLoginUser() instanceof AuthLoginUser viewer)) {
            throw new org.springframework.security.access.AccessDeniedException("请先登录");
        }
        var user = BeanUtil.copyProperties(viewer.getUser(), UserVO.class);
        user.setRoleIds(userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(viewer.getUserId()))
                .stream().map(UserRole::getRoleId).toList());
        return new com.nebula.auth.vo.CurrentSessionVO(user, viewer.getRoles(), viewer.getPermissions());
    }

    @Override
    public Long sendEmailCaptcha(String email) {
        email = RegistrationEmail.normalize(email);
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        String owner = registrationTokens.issueCaptcha(email, code);
        try {
            mailService.sendText(email, "Nebula Blog 注册验证码", "您的验证码为：" + code + "\n5分钟内有效，请勿泄露。");
        } catch (RuntimeException e) {
            registrationTokens.discardCaptcha(email, owner);
            throw new RegistrationUnavailableException(e);
        }
        return (long) abuseProtection.emailCooldownSeconds();
    }

    @Override
    public String verifyEmailCaptcha(EmailVerifyDTO dto) {
        com.nebula.common.util.PasswordPolicy.validateNewPassword(dto.getPassword());
        String email = RegistrationEmail.normalize(dto.getEmail());

        if (userMapper.selectCountByQuery(QueryWrapper.create().where(USER.EMAIL.eq(email))) > 0) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        }
        registrationTokens.requireCaptcha(email, dto.getCode());
        String verifyToken = registrationTokens.verifyAndIssue(email, dto.getCode(), passwordEncoder.encode(dto.getPassword()));


        // 发送确认邮件
        mailService.sendHtml(email, "Nebula Blog 注册确认", """
                <h3>Nebula Blog 注册确认</h3>
                <p>您正在注册 Nebula Blog 账号。</p>
                <p>请点击下面链接完成注册：</p>
                <a href="%s">
                完成注册
                </a>
                <p>该链接10分钟内有效。</p>
                """.formatted(org.springframework.web.util.HtmlUtils.htmlEscape(registration.confirmationLink(verifyToken))));
        return "验证成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean register(RegisterDTO dto) {
        RegisterCacheDTO cache = registrationTokens.consume(dto.getToken());
        try {
            User user = User.create()
                    .setNid("registering..." + UUID.randomUUID())
                    .setUsername("registering..." + UUID.randomUUID())
                    .setEmail(cache.getAccount()).setPassword(cache.getPassword())
                    .setStatus(UserStatus.NORMAL.getCode()).setDeleted(0);
            // 不同注册链接或并发请求仍由uk_user_email唯一约束阻止重复注册。
            if (userMapper.insertSelective(user) != 1) throw new RegistrationUnavailableException();
            user.setNid(String.valueOf(user.getId())).setUsername("普通用户" + user.getId());
            if (userMapper.update(user) != 1) throw new RegistrationUnavailableException();
            UserRole userRole = UserRole.create().setUserId(user.getId()).setRoleId(RoleEnum.USER.getRoleId());
            if (userRoleMapper.insertSelective(userRole) != 1) throw new RegistrationUnavailableException();
            return true;
        } catch (DuplicateKeyException e) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED);
        } catch (DataAccessException e) {
            throw new RegistrationUnavailableException(e);
        }
    }

    @Override
    public List<String> getUserPermissionsByUserId(Long userId) {
        return permissionService.getPermissionsByUserId(userId);
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        List<String> permissions = getUserPermissionsByUserId(userId);
        return permissions.contains(permission);
    }

    /** 将认证异常转换为项目登录结果码，用于一致记录失败原因。 */
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

    /** 按邮箱或NID查找账号，用于登录尝试计数与失败日志归属。 */
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
