package com.nebula.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.common.constant.UserStatus;
import com.nebula.common.constant.RoleEnum;
import com.nebula.common.security.RoleCatalog;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.UserErrorCode;
import com.nebula.common.util.SecurityUtils;
import com.nebula.common.util.PageLimits;
import com.nebula.user.dto.CancelAccountDTO;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.dto.UpdateNameDTO;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static com.nebula.common.exception.code.UserErrorCode.USER_ACCESS_DENIED;
import static com.nebula.common.exception.code.UserErrorCode.USER_SELF_OPERATION_DENIED;
import static com.nebula.common.exception.code.AuthErrorCode.PASSWORD_ERROR;
import static com.nebula.common.exception.code.RoleErrorCode.INVALID_ROLE_ASSIGNMENT;
import static com.nebula.common.exception.code.RoleErrorCode.LAST_SUPER_ADMIN_PROTECTED;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final UserRoleMapper userRoleMapper;
    private final com.nebula.common.security.SensitivePasswordVerifier sensitivePasswords;
    private final RoleCatalog roleCatalog;
    private final com.nebula.media.service.MediaService mediaService;

    @Override
    public Page<UserVO> pageUsers(String role, String username, int page, int size) {
        PageLimits.requireValid(page, size);
        QueryWrapper query = new QueryWrapper().where(USER.USERNAME.like(username, StringUtil::hasText)).orderBy(USER.ID.asc());
        if (StringUtil.hasText(role)) {
            var roleIds = roleCatalog.findRoleIds(role.strip());
            if (roleIds.isEmpty()) return Page.of(page, size, 0);
            query.and(USER.ID.in(QueryWrapper.create().select(USER_ROLE.USER_ID).from(USER_ROLE)
                    .where(USER_ROLE.ROLE_ID.in(roleIds))));
        }
        Page<User> userPage = page(Page.of(page, size), query);
        List<Long> userIds = userPage.getRecords().stream().map(User::getId).toList();

        List<UserRole> userRoles = userIds.isEmpty() ? List.of()
                : userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.in(userIds));
        Map<Long, List<Long>> roleMap = userRoles.stream().collect(Collectors.groupingBy(UserRole::getUserId, Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
        List<UserVO> voList = userPage.getRecords().stream().map(u -> {
            UserVO vo = BeanUtil.copyProperties(u, UserVO.class);
            vo.setRoleIds(roleMap.getOrDefault(u.getId(), List.of()));
            return vo;
        }).toList();
        Page<UserVO> userVOPage = Page.of(userPage.getPageNumber(), userPage.getPageSize(), userPage.getTotalRow());
        userVOPage.setRecords(voList);
        return userVOPage;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean switchStatusById(Long userId, Integer status) {
        if (java.util.Arrays.stream(UserStatus.values()).noneMatch(value -> value.matches(status))) {
            throw new BusinessException(UserErrorCode.USER_STATUS_INVALID);
        }
        checkSelfOrAdmin(userId);
        protectSuperAdminTarget(userId);
        if (!UserStatus.NORMAL.matches(status) && hasSuperAdminRole(userId)) {
            ensureAnotherActiveSuperAdmin(userId);
        }
        return updateById(User.create().setId(userId).setStatus(status));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean editUser(EditUserDTO dto) {
        SecurityUtils.requireSuperAdmin();
        if (SecurityUtils.isSelf(dto.getId())) {
            throw new BusinessException(USER_SELF_OPERATION_DENIED);
        }
        if (CollUtil.isEmpty(dto.getRoleIds())) {
            throw new BusinessException(UserErrorCode.USER_ROLE_EMPTY);
        }
        var roleIds = new LinkedHashSet<>(dto.getRoleIds());
        if (roleIds.contains(null) || !roleCatalog.allRolesExist(roleIds)) {
            throw new BusinessException(INVALID_ROLE_ASSIGNMENT);
        }
        if (getById(dto.getId()) == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        if (hasSuperAdminRole(dto.getId()) && !roleIds.contains(RoleEnum.SUPER_ADMIN.getRoleId())) {
            ensureAnotherActiveSuperAdmin(dto.getId());
        }
        if (!updateById(User.create().setId(dto.getId()).setUsername(dto.getUsername()))) {
            throw new BusinessException(UserErrorCode.USER_UPDATE_FAILED);
        }
        userRoleMapper.deleteByCondition(USER_ROLE.USER_ID.eq(dto.getId()));
        // 再批量插入新角色
        List<UserRole> userRoles = roleIds.stream().map(item -> UserRole.create().setUserId(dto.getId()).setRoleId(item)).toList();
        userRoleMapper.insertBatchSelective(userRoles);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUserById(Long id) {
        checkSelfOrAdmin(id);
        protectSuperAdminTarget(id);
        if (hasSuperAdminRole(id)) ensureAnotherActiveSuperAdmin(id);
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelCurrentAccount(CancelAccountDTO dto) {
        var viewer = SecurityUtils.getLoginUser();
        if (viewer == null) {
            throw new BusinessException(USER_ACCESS_DENIED);
        }
        if (!Boolean.TRUE.equals(dto.getConfirmed())) {
            throw new BusinessException(UserErrorCode.ACCOUNT_CANCELLATION_UNCONFIRMED);
        }
        User currentUser = mapper.selectOneById(viewer.getUserId());
        if (currentUser == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        if (!UserStatus.NORMAL.matches(currentUser.getStatus())) {
            throw new BusinessException(UserErrorCode.USER_DISABLED);
        }
        if (!sensitivePasswords.matches(dto.getPassword(), currentUser.getPassword())) {
            throw new BusinessException(PASSWORD_ERROR);
        }
        if (hasSuperAdminRole(viewer.getUserId())) ensureAnotherActiveSuperAdmin(viewer.getUserId());
        // MyBatis-Flex执行逻辑删除，同时匹配已验证的密码，阻止并发改密绕过确认。
        int affected = mapper.deleteByQuery(QueryWrapper.create()
                .where(USER.ID.eq(viewer.getUserId()))
                .and(USER.PASSWORD.eq(currentUser.getPassword()))
                .and(USER.STATUS.eq(UserStatus.NORMAL.getCode())));
        if (affected != 1) {
            throw new BusinessException(UserErrorCode.USER_DELETE_FAILED);
        }
        userRoleMapper.deleteByCondition(USER_ROLE.USER_ID.eq(viewer.getUserId()));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        checkSelfOrAdmin(userId);
        var user = mapper.selectOneByQuery(QueryWrapper.create().where(USER.ID.eq(userId)).forUpdate());
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        var asset = mediaService.upload("avatar", userId, file);
        if (!updateById(User.create().setId(userId).setAvatar(asset.getPath())))
            throw new BusinessException(UserErrorCode.USER_UPDATE_FAILED);
        // 旧头像保留在所属用户的媒体库中，由用户自行删除。
        return asset.getPath();
    }

    @Override
    public User getUserById(Long userId) { return getById(userId); }

    @Override
    public Boolean rename(UpdateNameDTO dto) {
        return updateById(User.create().setId(SecurityUtils.getUserId()).setUsername(dto.getUsername()));
    }

    @Override
    public List<UserVO> getUserByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return listAs(QueryWrapper.create().where(USER.ID.in(ids)), UserVO.class);
    }

    /**
     * 校验当前用户是否为本人或管理员
     *
     * @param userId 用户ID
     */
    private void checkSelfOrAdmin(Long userId) {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSelf(userId)) {
            throw new BusinessException(USER_ACCESS_DENIED);
        }
    }

    /** 检查目标用户是否直接绑定超级管理员角色，供账号层级保护复用。 */
    private boolean hasSuperAdminRole(Long userId) {
        return userRoleMapper.selectCountByCondition(USER_ROLE.USER_ID.eq(userId)
                .and(USER_ROLE.ROLE_ID.eq(RoleEnum.SUPER_ADMIN.getRoleId()))) > 0;
    }

    /** 阻止非超级管理员禁用或删除超级管理员账号。 */
    private void protectSuperAdminTarget(Long userId) {
        if (hasSuperAdminRole(userId) && !SecurityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("仅超级管理员可以管理超级管理员账号");
        }
    }

    /** 锁定顶层角色绑定和有效用户，保证并发操作也不能移除最后一位可用超级管理员。 */
    private void ensureAnotherActiveSuperAdmin(Long userId) {
        // 串行处理有效超级管理员的移除，避免并发请求同时移除最后的管理员。
        var rootBindings = userRoleMapper.selectListByQuery(QueryWrapper.create()
                .where(USER_ROLE.ROLE_ID.eq(RoleEnum.SUPER_ADMIN.getRoleId()))
                .orderBy(USER_ROLE.USER_ID.asc()).forUpdate());
        var rootUserIds = rootBindings.stream().map(UserRole::getUserId).distinct().toList();
        if (rootUserIds.isEmpty()) throw new BusinessException(LAST_SUPER_ADMIN_PROTECTED);
        var activeSuperAdmins = mapper.selectListByQuery(QueryWrapper.create()
                .where(USER.ID.in(rootUserIds))
                .and(USER.STATUS.eq(UserStatus.NORMAL.getCode())).orderBy(USER.ID.asc()).forUpdate());
        if (activeSuperAdmins.stream().allMatch(user -> user.getId().equals(userId))) {
            throw new BusinessException(LAST_SUPER_ADMIN_PROTECTED);
        }
    }
}
