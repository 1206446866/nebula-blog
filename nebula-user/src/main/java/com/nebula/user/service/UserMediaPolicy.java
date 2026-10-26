package com.nebula.user.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.UserErrorCode;
import com.nebula.media.service.MediaOwnerResolver;
import com.nebula.media.service.MediaReferences;
import com.nebula.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import static com.nebula.user.entity.table.UserTableDef.USER;

@Component
@RequiredArgsConstructor
public class UserMediaPolicy implements MediaOwnerResolver, MediaReferences {
    private final UserMapper users;
    @Override public String requireNid(Long id) {
        var user = users.selectOneById(id);
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        return user.getNid();
    }
    @Override public List<Reference> find(String path) {
        return users.selectListByQuery(QueryWrapper.create().where(USER.AVATAR.eq(path)))
                .stream().map(user -> new Reference("avatar", user.getId(), "当前头像", true, true)).toList();
    }
}
