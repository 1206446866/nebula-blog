package com.nebula.user.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserProfileVO;
import com.nebula.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Value("${file.upload-dir}")
    private String uploadDir;

//TODO
    @Override
    public Page<UserVO> pageUsers(String role, String username, int page, int size) {
//        QueryWrapper query = new QueryWrapper().where(USER.ROLE.eq(role, StringUtil::hasText)).and(USER.USERNAME.like(username, StringUtil::hasText)).orderBy(USER.ID.asc());
//        return pageAs(Page.of(page, size), query, UserVO.class);
        return null;
    }


    @Override
    public Boolean switchStatusById(Long userId, Integer status) {
        return updateById(User.create().setId(userId).setStatus(status));
    }
//TODO
    @Override
    public boolean editUser(EditUserDTO dto) {
//        return updateById(User.create().setId(dto.getId()).setUsername(dto.getUsername()).setRole(dto.getRole()));
        return false;
    }

    @Override
    public Boolean deleteUserById(Long id) {
        return removeById(id);
    }

    @Override
    public UserProfileVO getProfile(Long id) {
        UserProfileVO userProfileVO = getOneAs(QueryWrapper.create().where(USER.ID.eq(id)), UserProfileVO.class);
        if (userProfileVO == null) {
            throw new RuntimeException("用户不存在");
        }
        return userProfileVO;
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
//TODO
    @Override
    public UserVO getUserInfo(Long userId) {
//        User user = getById(userId);
        return null;
//        return UserVO.create().setId(user.getId()).setUsername(user.getUsername()).setAvatar(user.getAvatar()).setRole(user.getRole());
    }
}