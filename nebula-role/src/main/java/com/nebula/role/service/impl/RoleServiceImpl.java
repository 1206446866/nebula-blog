package com.nebula.role.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.role.dto.CreateRoleDto;
import com.nebula.role.dto.RolePageDto;
import com.nebula.role.entity.Role;
import com.nebula.role.mapper.RoleMapper;
import com.nebula.role.service.RoleService;
import com.nebula.role.vo.RoleVO;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.nebula.role.entity.table.RoleTableDef.ROLE;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final UserRoleMapper userRoleMapper;

    @Override
    public List<RoleVO> findAll() {
        return listAs(QueryWrapper.create(), RoleVO.class);
    }

    @Override
    public Page<RoleVO> pageRoles(RolePageDto dto) {
        QueryWrapper query = new QueryWrapper();
        query.where(ROLE.DESCRIPTION.like(dto.getDescription(), !dto.getDescription().isEmpty()));
        query.orderBy(ROLE.CREATE_TIME.asc());
        return this.pageAs(Page.of(dto.getCurrent(), dto.getSize()), query, RoleVO.class);
    }


    @Override
    public List<Role> getRolesByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(userId)).stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) return List.of();
        return getMapper().selectListByIds(roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeRole(Long roleId) {
         userRoleMapper.deleteByCondition(USER_ROLE.ROLE_ID.eq(roleId));
        return remove(ROLE.ID.eq(roleId));
    }

    @Override
    public Boolean createRole(CreateRoleDto dto) {
        return save(Role.create().setName(dto.getName()).setDescription(dto.getDescription()));
    }

    @Override
    public List<String> getDescriptions(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(userId));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        List<Role> roleList = getMapper().selectListByQuery(QueryWrapper.create().select(ROLE.DESCRIPTION).where(ROLE.ID.in(roleIds, !roleIds.isEmpty())));
        return roleList.stream().map(Role::getDescription).toList();
    }
}