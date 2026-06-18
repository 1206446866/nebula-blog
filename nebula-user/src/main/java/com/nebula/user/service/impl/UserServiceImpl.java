package com.nebula.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.UserErrorCode;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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


    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        int index = originalFilename.lastIndexOf('.');

        if (index < 0) {
            throw new RuntimeException("文件格式错误");
        }

        String suffix = originalFilename.substring(index).toLowerCase();
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            throw new RuntimeException("不支持的图片格式");
        }

        // TODO 真实内容校验

        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + UUID.randomUUID() + suffix;
        String dir = System.getProperty("user.dir") + File.separator + uploadDir;
        File folder = new File(dir);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("上传失败");
        }
        if (file.isEmpty()) {
            throw new RuntimeException("请选择文件");
        }
        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("图片不能超过10MB");
        }
        try {
            File target = new File(folder, fileName);
            Files.copy(file.getInputStream(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("上传失败", e);
        }
        // 更新数据库
        updateById(User.create().setId(userId).setAvatar(fileName));

        return fileName;
    }

    @Override
    public User getUserById(Long userId) {
        return getById(userId);
    }
}