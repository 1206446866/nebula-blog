package com.nebula.role.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.role.dto.CreateRoleDto;
import com.nebula.role.dto.RolePageDto;
import com.nebula.role.dto.UpdateRoleDto;
import com.nebula.common.constant.RoleEnum;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.RoleCatalog;
import com.nebula.common.util.SecurityUtils;
import com.nebula.common.util.PageLimits;
import com.nebula.role.mapper.RolePermissionMapper;
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
import java.util.Set;
import java.util.Objects;

import static com.nebula.role.entity.table.RoleTableDef.ROLE;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.role.entity.table.RolePermissionTableDef.ROLE_PERMISSION;
import static com.nebula.common.exception.code.RoleErrorCode.*;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService, RoleCatalog {

    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<RoleVO> findAll() {
        return listAs(QueryWrapper.create(), RoleVO.class);
    }

    @Override
    public Page<RoleVO> pageRoles(RolePageDto dto) {
        PageLimits.requireValid(dto.getCurrent(), dto.getSize());
        QueryWrapper query = new QueryWrapper();
        query.where(ROLE.DESCRIPTION.like(dto.getDescription(), org.springframework.util.StringUtils.hasText(dto.getDescription())));
        query.orderBy(ROLE.CREATE_TIME.asc(), ROLE.ID.asc());
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
        SecurityUtils.requireSuperAdmin();
        Role role = mapper.selectOneByQuery(QueryWrapper.create().where(ROLE.ID.eq(roleId)).forUpdate());
        if (role == null) throw new BusinessException(ROLE_NOT_FOUND);
        if (RoleEnum.fromCode(role.getName()) != null || RoleEnum.fromId(roleId) != null) {
            throw new BusinessException(BUILT_IN_ROLE_PROTECTED);
        }
        userRoleMapper.deleteByCondition(USER_ROLE.ROLE_ID.eq(roleId));
        rolePermissionMapper.deleteByCondition(ROLE_PERMISSION.ROLE_ID.eq(roleId));
        return remove(ROLE.ID.eq(roleId));
    }

    @Override
    public Boolean createRole(CreateRoleDto dto) {
        SecurityUtils.requireSuperAdmin();
        if (count(ROLE.NAME.eq(dto.getName())) > 0) throw new BusinessException(ROLE_NAME_EXISTS);
        return save(Role.create().setName(dto.getName()).setDescription(dto.getDescription()));
    }

    @Override
    public Boolean updateRole(UpdateRoleDto dto) {
        SecurityUtils.requireSuperAdmin();
        Role role = getById(dto.getId());
        if (role == null) throw new BusinessException(ROLE_NOT_FOUND);
        if ((RoleEnum.fromCode(role.getName()) != null || RoleEnum.fromId(role.getId()) != null)
                && !Objects.equals(role.getName(), dto.getName())) {
            throw new BusinessException(BUILT_IN_ROLE_PROTECTED);
        }
        if (count(ROLE.NAME.eq(dto.getName()).and(ROLE.ID.ne(dto.getId()))) > 0) {
            throw new BusinessException(ROLE_NAME_EXISTS);
        }
        return updateById(Role.create().setId(dto.getId()).setName(dto.getName()).setDescription(dto.getDescription()));
    }

    @Override
    public boolean allRolesExist(Set<Long> roleIds) {
        return !roleIds.isEmpty() && mapper.selectListByQuery(QueryWrapper.create()
                .where(ROLE.ID.in(roleIds)).orderBy(ROLE.ID.asc()).forUpdate()).size() == roleIds.size();
    }

    @Override
    public Set<Long> findRoleIds(String filter) {
        QueryWrapper query = QueryWrapper.create().select(ROLE.ID);
        if (filter.matches("[0-9]+")) {
            try { query.where(ROLE.ID.eq(Long.parseLong(filter))); }
            catch (NumberFormatException ignored) { return Set.of(); }
        } else {
            query.where(ROLE.NAME.like(filter).or(ROLE.DESCRIPTION.like(filter)));
        }
        return mapper.selectListByQuery(query).stream().map(Role::getId).collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public List<String> getDescriptions(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(userId));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) return List.of();
        List<Role> roleList = getMapper().selectListByQuery(QueryWrapper.create().select(ROLE.DESCRIPTION).where(ROLE.ID.in(roleIds)));
        return roleList.stream().map(Role::getDescription).toList();
    }
}
