package com.nebula.user.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.user.dto.CancelAccountDTO;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.dto.UpdateNameDTO;
import com.nebula.user.entity.User;
import com.nebula.user.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/** 用户查询、账号管理和个人资料服务。 */
public interface UserService extends IService<User> {

    /**
     * 按角色和用户名筛选用户，返回完整分页信息且不重复用户。
     *
     * @param role 角色ID、编码或说明，可为空
     * @param username 用户名关键词，可为空
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 用户分页结果
     */
    Page<UserVO> pageUsers(String role, String username, int page, int size);

    /**
     * 校验操作层级后修改用户状态，保护最后一位可用超级管理员。
     *
     * @param userId 目标用户ID
     * @param status 目标状态：0正常、1禁用、2锁定
     * @return 是否修改成功
     */
    Boolean switchStatusById(Long userId, Integer status);

    /**
     * 由超级管理员修改其他用户的昵称和角色，先校验再替换角色绑定。
     *
     * @param dto 用户ID、昵称与完整角色列表
     * @return 是否修改成功
     */
    boolean editUser(EditUserDTO dto);

    /**
     * 校验本人或管理员权限后逻辑删除用户，并保护顶层管理账号。
     *
     * @param id 目标用户ID
     * @return 是否删除成功
     */
    Boolean deleteUserById(Long id);

    /**
     * 使用共享失败预算校验当前密码和明确确认后注销本人账号，保留已发表内容；兼容历史短密码。
     *
     * @param dto 当前密码与注销确认
     * @return 是否注销成功
     */
    Boolean cancelCurrentAccount(CancelAccountDTO dto);



    /**
     * 上传头像并更新用户记录，旧头像保留在媒体库中。
     *
     * @param userId 头像所属用户ID
     * @param file 待上传图片
     * @return upload/avatar/NID/日期/文件名格式的相对路径
     * @throws IOException 图片文件读写失败
     */
    String uploadAvatar(Long userId, MultipartFile file) throws IOException;

    /**
     * 读取未删除的用户实体，返回值仅供服务内部使用。
     *
     * @param userId 用户ID
     * @return 用户实体，不存在时为null
     */
    User getUserById(Long userId);

    /**
     * 修改当前登录用户的显示昵称，不修改登录NID。
     *
     * @param dto 新昵称
     * @return 是否修改成功
     */
    Boolean rename(UpdateNameDTO dto);

    /**
     * 批量查询未删除用户的公开展示资料，空列表不查询。
     *
     * @param ids 用户ID列表
     * @return 不含密码的用户展示列表
     */
    List<UserVO> getUserByIds(List<Long> ids);
}
