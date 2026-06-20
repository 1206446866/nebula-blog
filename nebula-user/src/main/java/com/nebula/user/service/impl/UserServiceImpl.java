package com.nebula.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.common.constant.NumberConstant;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.UserErrorCode;
import com.nebula.common.util.SecurityUtils;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, com.nebula.user.entity.User> implements UserService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final UserRoleMapper userRoleMapper;

    @Override
    public Page<UserVO> pageUsers(String role, String username, int page, int size) {
        QueryWrapper query = new QueryWrapper().where(USER.USERNAME.like(username, StringUtil::hasText)).orderBy(USER.ID.asc());
        // 如果传了 role，需要 join user_role + role
        if (StringUtil.hasText(role)) {
            query.innerJoin(USER_ROLE).on(USER.ID.eq(USER_ROLE.USER_ID));
        }
        Page<User> userPage = page(Page.of(page, size), query);
        List<Long> userIds = userPage.getRecords().stream().map(User::getId).toList();

        List<UserRole> userRoles = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.in(userIds));
        Map<Long, List<Long>> roleMap = userRoles.stream().collect(Collectors.groupingBy(UserRole::getUserId, Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
        List<UserVO> voList = userPage.getRecords().stream().map(u -> {
            UserVO vo = BeanUtil.copyProperties(u, UserVO.class);
            vo.setRoleIds(roleMap.getOrDefault(u.getId(), List.of()));
            return vo;
        }).toList();
        Page<UserVO> userVOPage = new Page<>();
        userVOPage.setRecords(voList);
        userVOPage.setTotalPage(userPage.getTotalPage());
        return userVOPage;
    }


    @Override
    public Boolean switchStatusById(Long userId, Integer status) {
        return updateById(User.create().setId(userId).setStatus(status));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean editUser(EditUserDTO dto) {
        updateById(User.create().setId(dto.getId()).setUsername(dto.getUsername()));
        userRoleMapper.deleteByCondition(USER_ROLE.USER_ID.eq(dto.getId()));
        if (CollUtil.isEmpty(dto.getRoleIds())) {
            throw new BusinessException(UserErrorCode.USER_ROLE_EMPTY);
        }
        List<UserRole> userRoles = dto.getRoleIds().stream().map(item -> UserRole.create().setUserId(dto.getId()).setRoleId(item)).toList();
        userRoleMapper.insertBatchSelective(userRoles);
        return true;
    }

    @Override
    public Boolean deleteUserById(Long id) {
        return removeById(id);
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
            throw new RuntimeException("只能上传图片");
        }
        if (file.getSize() > NumberConstant.MAX_AVATAR_SIZE) {
            throw new RuntimeException("图片不能超过10MB");
        }
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String oldAvatar = user.getAvatar();
        File target = new File(folder, fileName);
        // 图片内容校验（防止伪造图片文件）
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new RuntimeException("图片内容非法");
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
            throw new RuntimeException("请选择文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        int index = originalFilename.lastIndexOf('.');

        if (index < 0) {
            throw new RuntimeException("文件格式错误");
        }

        String suffix = originalFilename.substring(index+1).toLowerCase();
        if (!suffix.matches("jpg|jpeg|png|gif|webp")) {
            throw new RuntimeException("不支持的图片格式");
        }
        return "."+suffix;
    }


    @Override
    public Boolean updateSelfName(UpdateNameDTO dto) {
        return updateById(User.create().setId(SecurityUtils.getUserId()).setUsername(dto.getUsername()));
    }
}