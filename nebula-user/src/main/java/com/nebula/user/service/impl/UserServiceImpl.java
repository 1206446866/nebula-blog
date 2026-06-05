package com.nebula.user.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserProfileVO;
import com.nebula.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Page<User> pageUsers(String role, String username, int page, int size) {
        QueryWrapper query = new QueryWrapper()
                .where(USER.ROLE.eq(role, StringUtil::hasText))
                .and(USER.USERNAME.eq(username, StringUtil::hasText))
                .orderBy(USER.ID.asc());
        return page(Page.of(page, size), query);
    }

    @Override
    public long countUsers(String role, String username) {
        QueryWrapper query = new QueryWrapper().where(USER.ROLE.eq(role, StringUtil::hasText).and(USER.USERNAME.eq(username, StringUtils::hasText)));
        return count(query);
    }

    @Override
    public Page<User> getUsersByRole(String role, long current, long size) {
        QueryWrapper query = new QueryWrapper();
        query.select(USER.ID, USER.USERNAME, USER.ROLE, USER.STATUS, USER.CREATE_TIME)
                .where(USER.ROLE.eq(role, StringUtil::hasText))
                .orderBy(USER.ID.asc());
        return page(Page.of(current, size), query);
    }

    @Override
    public Boolean switchStatusById(Long userId, Integer status) {
        return updateById(User.create().setId(userId).setStatus(status));
    }

    @Override
    public boolean addUser(User user) {
        return save(user);
    }

    @Override
    public boolean updateUser(User user) {
        return updateById(user);
    }

    //TODO
    @Override
    public boolean changePassword(Long userId, String newPassword) {
        return false;
    }

    //TODO
    @Override
    public boolean resetPassword(Long userId) {
        return false;
    }

    @Override
    public Boolean deleteUserById(Long id) {
        return removeById(id);
    }

    @Override
    public UserProfileVO getProfile(Long id) {
        UserProfileVO userProfileVO =getOneAs(QueryWrapper.create().where(USER.ID.eq(id)), UserProfileVO.class);
        if (userProfileVO == null) {throw new RuntimeException("用户不存在");}
        return userProfileVO;
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        String fileName = UUID.randomUUID() + ".png";
        String dir = "upload/avatar/";
        File folder = new File(dir);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("上传失败");
        }

        try {
            file.transferTo(new File(folder, fileName));
        } catch (Exception e) {
            throw new RuntimeException("上传失败",e);
        }
        // 更新数据库
        String avatarUrl = "/avatar/" + fileName;
        updateById(User.create().setId(userId).setAvatar(avatarUrl));

        // 返回 URL
        return avatarUrl;
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = getById(userId);

        return UserVO.create()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setAvatar(user.getAvatar())
                .setRole(user.getRole());
    }
}