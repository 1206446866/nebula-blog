package com.nebula.user.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.StringUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.common.util.SecurityUtils;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserProfileVO;
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

    private final SecurityUtils securityUtils;

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

//TODO
    @Override
    public String uploadAvatar(MultipartFile file) {

        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 文件名
        String original = file.getOriginalFilename();
        String suffix = original.substring(original.lastIndexOf("."));

        String fileName = UUID.randomUUID() + suffix;

        String dir = "upload/avatar/";

        File target = new File(dir + fileName);

        target.getParentFile().mkdirs();

        try {
            file.transferTo(target);
        } catch (Exception e) {
            throw new RuntimeException("上传失败");
        }
//        Long userId = securityUtils.getLoginUser().getUserId();
//        User user = getById(userId);
//        user.setAvatar(url);

//        updateById(user);
        // 返回访问路径（你可以换成Nginx域名）
        return "/static/avatar/" + fileName;
    }
}