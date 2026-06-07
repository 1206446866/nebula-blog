package com.nebula.user.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.user.entity.User;
import com.nebula.user.vo.UserProfileVO;
import com.nebula.user.vo.UserVO;
import jakarta.validation.constraints.Min;
import org.springframework.web.multipart.MultipartFile;


public interface UserService extends IService<User> {

    /**
     * 分页查询用户，可根据角色和用户名模糊搜索
     *
     * @param page 当前页
     * @param size 每页数量
     * @param role 角色，可选
     * @param username 用户名，可选
     * @return 用户分页列表
     */
    Page<User> pageUsers(String role, String username, int page, int size);

    long countUsers(String role, String username);

    // 根据角色查询用户
    Page<User> getUsersByRole(String role,long current,long size);

    /**
     * 修改用户状态（启用/禁用）
     *
     * @param userId 用户ID
     * @param status 新状态，1=启用, 0=禁用
     * @return 操作是否成功
     */
    Boolean switchStatusById(Long userId, Integer status);


    /**
     * 修改用户密码（用户自己操作）
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 操作是否成功
     */
    Boolean changePassword(Long userId, String newPassword);

    /**
     * 管理员重置用户密码
     *
     * @param userId 用户ID
     * @return 操作是否成功
     */
    Boolean resetPassword(Long userId);

    /**
     * 删除用户
     * @param id 用户ID
     * @return 操作是否成功
     */
    Boolean deleteUserById(Long id);


    /**
     * 获取用户主页信息
     *
     * <p>
     * 包含用户基础信息、文章统计、评论统计、
     * 总浏览量以及最近发布的文章列表。
     * </p>
     *
     * @param id 用户ID
     * @return 用户主页信息
     */
    UserProfileVO getProfile(@Min(value = 1, message = "用户ID非法") Long id);

    /**
     * 上传用户头像
     *
     * @param userId 用户ID
     * @param file   文件
     * @return 头像URL
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     *
     * @param userId 用户ID
     * @return
     */
    UserVO getUserInfo(Long userId);
}