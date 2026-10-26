package com.nebula.user.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.common.constant.UserStatus;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.PageLimits;
import com.nebula.common.util.SecurityUtils;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserFollow;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserFollowMapper;
import com.nebula.user.service.UserFollowService;
import com.nebula.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import static com.nebula.common.exception.code.UserErrorCode.*;
import static com.nebula.user.entity.table.UserTableDef.USER;
import static com.nebula.user.entity.table.UserFollowTableDef.USER_FOLLOW;

@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {
    private final UserMapper users;
    private final UserFollowMapper follows;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FollowSummaryVO setFollowing(Long targetId, boolean following) {
        Long actorId = viewerId();
        if (actorId.equals(targetId)) throw new BusinessException(USER_SELF_OPERATION_DENIED);
        // 固定顺序锁定双方用户；并发重复请求、互相关注和账号删除都不能留下重复有效关系。
        var locked = users.selectListByQuery(QueryWrapper.create().where(USER.ID.in(actorId, targetId))
                .orderBy(USER.ID.asc()).forUpdate());
        User actor = locked.stream().filter(u -> actorId.equals(u.getId())).findFirst()
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        User target = locked.stream().filter(u -> targetId.equals(u.getId())).findFirst()
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        if (!UserStatus.NORMAL.matches(actor.getStatus()) || (following && !UserStatus.NORMAL.matches(target.getStatus())))
            throw new BusinessException(USER_DISABLED);
        var condition = USER_FOLLOW.FOLLOWER_ID.eq(actorId).and(USER_FOLLOW.FOLLOWING_ID.eq(targetId));
        if (following) {
            if (follows.selectCountByCondition(condition) == 0)
                follows.insert(new UserFollow().setFollowerId(actorId).setFollowingId(targetId));
        } else follows.deleteByCondition(condition);
        return summary(targetId);
    }

    @Override
    public FollowSummaryVO summary(Long userId) {
        Long viewer = viewerId();
        User target = requireUser(userId);
        boolean self = viewer.equals(userId);
        return new FollowSummaryVO(countConnections(userId, false), countConnections(userId, true),
                !self && exists(viewer, userId), !self && exists(userId, viewer), self,
                !self && UserStatus.NORMAL.matches(target.getStatus()));
    }

    @Override
    public Page<CommunityUserVO> connections(Long userId, boolean followers, int current, int size) {
        PageLimits.requireValid(current, size);
        requireUser(userId);
        var otherId = followers ? USER_FOLLOW.FOLLOWER_ID : USER_FOLLOW.FOLLOWING_ID;
        var ownerId = followers ? USER_FOLLOW.FOLLOWING_ID : USER_FOLLOW.FOLLOWER_ID;
        // 从用户表分页保证逻辑删除生效，联合主键保证每名用户只出现一次。
        var query = QueryWrapper.create().select(USER.ALL_COLUMNS).from(USER)
                .innerJoin(USER_FOLLOW).on(USER.ID.eq(otherId)).where(ownerId.eq(userId))
                .orderBy(USER_FOLLOW.CREATE_TIME.desc(), USER.ID.desc());
        return pageUsers(query, current, size);
    }

    @Override
    public Page<CommunityUserVO> discover(String keyword, int current, int size) {
        PageLimits.requireValid(current, size);
        var query = QueryWrapper.create().where(USER.STATUS.eq(UserStatus.NORMAL.getCode()))
                .and(USER.ID.ne(viewerId()));
        if (keyword != null && !keyword.isBlank()) {
            String term = keyword.strip();
            if (term.length() > 50) throw new BusinessException(COMMUNITY_KEYWORD_TOO_LONG);
            query.and(USER.USERNAME.like(term).or(USER.NID.like(term)));
        }
        return pageUsers(query.orderBy(USER.CREATE_TIME.desc(), USER.ID.desc()), current, size);
    }

    /** 计数时联查另一端的用户状态，排除已注销用户；禁用用户的既有关系仍保留。 */
    private long countConnections(Long userId, boolean followers) {
        var otherId = followers ? USER_FOLLOW.FOLLOWER_ID : USER_FOLLOW.FOLLOWING_ID;
        var ownerId = followers ? USER_FOLLOW.FOLLOWING_ID : USER_FOLLOW.FOLLOWER_ID;
        return follows.selectCountByQuery(QueryWrapper.create().from(USER_FOLLOW)
                .innerJoin(USER).on(USER.ID.eq(otherId)).where(ownerId.eq(userId)).and(USER.DELETED.eq(0)));
    }

    /** 每页批量读取双向关系，避免为每个用户各查询一次；只映射公开资料。 */
    private Page<CommunityUserVO> pageUsers(QueryWrapper query, int current, int size) {
        Long viewer = viewerId();
        Page<User> page = users.paginate(current, size, query);
        if (page.getTotalPage() > 0 && current > page.getTotalPage())
            page = users.paginate(page.getTotalPage(), size, query);
        var ids = page.getRecords().stream().map(User::getId).toList();
        Set<Long> outgoing = Set.of(), incoming = Set.of();
        if (!ids.isEmpty()) {
            outgoing = follows.selectListByQuery(QueryWrapper.create().where(USER_FOLLOW.FOLLOWER_ID.eq(viewer))
                    .and(USER_FOLLOW.FOLLOWING_ID.in(ids))).stream().map(UserFollow::getFollowingId).collect(Collectors.toSet());
            incoming = follows.selectListByQuery(QueryWrapper.create().where(USER_FOLLOW.FOLLOWING_ID.eq(viewer))
                    .and(USER_FOLLOW.FOLLOWER_ID.in(ids))).stream().map(UserFollow::getFollowerId).collect(Collectors.toSet());
        }
        Set<Long> followingIds = outgoing, followerIds = incoming;
        return page.map(user -> new CommunityUserVO(user.getId(), user.getNid(), user.getUsername(), user.getAvatar(),
                followingIds.contains(user.getId()), followerIds.contains(user.getId()), viewer.equals(user.getId()),
                !viewer.equals(user.getId()) && UserStatus.NORMAL.matches(user.getStatus())));
    }

    /** 验证主页用户存在；MyBatis-Flex 自动排除逻辑删除账号。 */
    private User requireUser(Long id) {
        User user = users.selectOneById(id);
        if (user == null) throw new BusinessException(USER_NOT_FOUND);
        return user;
    }

    /** 查询指定方向的关系，不接受由客户端提供的操作者身份。 */
    private boolean exists(Long from, Long to) {
        return follows.selectCountByCondition(USER_FOLLOW.FOLLOWER_ID.eq(from).and(USER_FOLLOW.FOLLOWING_ID.eq(to))) > 0;
    }

    /** 服务层也要求登录，避免绕过控制器后使用匿名身份。 */
    private Long viewerId() {
        var viewer = SecurityUtils.getLoginUser();
        if (viewer == null) throw new AccessDeniedException("请先登录");
        return viewer.getUserId();
    }
}
