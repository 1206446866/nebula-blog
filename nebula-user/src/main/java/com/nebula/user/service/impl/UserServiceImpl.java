package com.nebula.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.common.constant.NumberConstant;
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
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
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

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleCatalog roleCatalog;

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
        if (dto.getPassword() == null || dto.getPassword().isBlank()
                || !passwordEncoder.matches(dto.getPassword(), currentUser.getPassword())) {
            throw new BusinessException(PASSWORD_ERROR);
        }
        if (hasSuperAdminRole(viewer.getUserId())) ensureAnotherActiveSuperAdmin(viewer.getUserId());
        // MyBatis-Flex applies User.deleted logical deletion. Match the verified
        // password too, so a concurrent password change cannot bypass this check.
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

    /**
     * 用户头像上传
     * <p>
     * 流程：
     * 1. 校验文件基本合法性（后缀、大小、类型）
     * 2. 校验用户是否存在
     * 3. 读取图片内容进行二次验证（防伪造文件）
     * 4. 写入本地磁盘
     * 5. 更新数据库头像字段
     * 6. 更新失败则回滚文件写入
     * 7. 删除旧头像文件
     * <p>
     * 注意：
     * - 文件名使用 UUID + 时间戳，避免冲突
     * - 文件存储在本地 uploadDir 目录下
     * - ImageIO 用于校验图片内容是否真实可解析
     * @param userId 用户ID
     * @param file   文件
     * @return 文件名称
     */
    @Override
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        checkSelfOrAdmin(userId);
        // 校验文件后缀（并返回标准化后缀）
        String suffix = validateAndGetSuffix(file);
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + UUID.randomUUID() + suffix;
        // 获取存储目录
        File folder = new File(getUploadRoot());
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("上传失败，文件夹无法创建");
        }
        byte[] bytes = file.getBytes();
        String contentType = file.getContentType();
        // 基于HTTP声明的类型做快速过滤
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(UserErrorCode.AVATAR_TYPE_INVALID);
        }
        if (file.getSize() > NumberConstant.MAX_AVATAR_SIZE) {
            throw new BusinessException(UserErrorCode.AVATAR_TOO_LARGE);
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        String oldAvatar = user.getAvatar();
        File target = new File(folder, fileName);
        // 图片内容校验（防止伪造图片文件）
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new BusinessException(UserErrorCode.AVATAR_CONTENT_INVALID);
        }
        try {
            Files.copy(new ByteArrayInputStream(bytes), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("上传失败", e);
        }
        // 更新数据库, DB更新失败则删除已上传文件，避免脏数据
        try {
            boolean success = updateById(User.create().setId(userId).setAvatar(fileName));
            if (!success) {
                Files.deleteIfExists(target.toPath());
                throw new RuntimeException("更新头像失败");
            }
        } catch (Exception e) {
            Files.deleteIfExists(target.toPath());
            throw e;
        }
        // 删除旧头像文件（忽略失败）
        deleteOldAvatar(oldAvatar);
        return fileName;
    }


    @Override
    public User getUserById(Long userId) {
        return getById(userId);
    }

    private String getUploadRoot() {
        return System.getProperty("user.dir") + File.separator + uploadDir;
    }

    private void deleteOldAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return;
        }
        if ("default.png".equals(avatar)) {
            return;
        }
        File file = new File(getUploadRoot(), avatar);

        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException("删除旧头像失败: ", e);
        }
    }

    @Nonnull
    private static String validateAndGetSuffix(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(UserErrorCode.AVATAR_FILE_REQUIRED);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(UserErrorCode.AVATAR_FILE_REQUIRED);
        }

        int index = originalFilename.lastIndexOf('.');

        if (index < 0) {
            throw new BusinessException(UserErrorCode.AVATAR_TYPE_INVALID);
        }

        String suffix = originalFilename.substring(index+1).toLowerCase();
        if (!suffix.matches("jpg|jpeg|png|gif|webp")) {
            throw new BusinessException(UserErrorCode.AVATAR_TYPE_INVALID);
        }
        return "."+suffix;
    }


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

    private boolean hasSuperAdminRole(Long userId) {
        return userRoleMapper.selectCountByCondition(USER_ROLE.USER_ID.eq(userId)
                .and(USER_ROLE.ROLE_ID.eq(RoleEnum.SUPER_ADMIN.getRoleId()))) > 0;
    }

    private void protectSuperAdminTarget(Long userId) {
        if (hasSuperAdminRole(userId) && !SecurityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("仅超级管理员可以管理超级管理员账号");
        }
    }

    private void ensureAnotherActiveSuperAdmin(Long userId) {
        // Serialize removals of active super administrators so concurrent requests
        // cannot both remove the last remaining administrators.
        var rootBindings = userRoleMapper.selectListByQuery(QueryWrapper.create()
                .where(USER_ROLE.ROLE_ID.eq(RoleEnum.SUPER_ADMIN.getRoleId()))
                .orderBy(USER_ROLE.USER_ID.asc()).forUpdate());
        var rootUserIds = rootBindings.stream().map(UserRole::getUserId).distinct().toList();
        if (rootUserIds.isEmpty()) throw new BusinessException(LAST_SUPER_ADMIN_PROTECTED);
        var activeSuperAdmins = mapper.selectListByQuery(QueryWrapper.create()
                .where(USER.ID.in(rootUserIds))
                .and(USER.STATUS.eq(UserStatus.NORMAL.getCode())).orderBy(USER.ID.asc()).forUpdate());
        if (activeSuperAdmins.stream().noneMatch(user -> !user.getId().equals(userId))) {
            throw new BusinessException(LAST_SUPER_ADMIN_PROTECTED);
        }
    }
}
