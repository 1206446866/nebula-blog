package com.nebula.user.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.entity.User;
import com.nebula.user.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface UserService extends IService<User> {

    /**
     * 分页查询用户，可根据角色和用户名模糊搜索
     *
     * @param page     当前页
     * @param size     每页数量
     * @param role     角色，可选
     * @param username 用户名，可选
     * @return 用户分页列表
     */
    Page<UserVO> pageUsers(String role, String username, int page, int size);

    /**
     * 修改用户状态（启用/禁用）
     *
     * @param userId 用户ID
     * @param status 新状态，1=启用, 0=禁用
     * @return 操作是否成功
     */
    Boolean switchStatusById(Long userId, Integer status);

    boolean editUser(EditUserDTO dto);

    /**
     * 删除用户
     * @param id 用户ID
     * @return 操作是否成功
     */
    Boolean deleteUserById(Long id);



    /**
     * 上传用户头像
     *
     * @param userId 用户ID
     * @param file   文件
     * @return 头像URL
     */
    String uploadAvatar(Long userId, MultipartFile file) throws IOException;

    /**
     * @param userId 用户ID
     */
    User getUserById(Long userId);
}