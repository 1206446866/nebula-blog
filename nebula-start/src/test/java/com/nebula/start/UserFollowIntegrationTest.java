package com.nebula.start;

import com.nebula.auth.util.JwtUtil;
import com.nebula.user.entity.*;
import com.nebula.user.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.nebula.user.entity.table.UserFollowTableDef.USER_FOLLOW;

/** 仅操作每个测试新建的独立账号；所有数据库变更回滚，测试 Token 单独撤销。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserFollowIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired UserMapper users;
    @Autowired UserFollowMapper follows;
    @Autowired JwtUtil jwt;
    private final List<String> tokens = new ArrayList<>();

    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
    }
    private User user() {
        User user = User.create().setNid("follow_test_" + UUID.randomUUID().toString().replace("-", ""))
                .setUsername("关注测试").setPassword("unused").setEmail(UUID.randomUUID() + "@example.invalid")
                .setPhone(null).setStatus(0).setDeleted(0);
        users.insert(user);
        return user;
    }
    private String auth(User user) {
        String token = jwt.createToken(user); tokens.add(token); return "Bearer " + token;
    }
    private void edge(User from, User to) {
        follows.insert(new UserFollow().setFollowerId(from.getId()).setFollowingId(to.getId()));
    }
    private String path(User user) { return "/community/users/" + user.getId(); }

    @Test void anonymousCannotReadOrChangeRelationships() throws Exception {
        User target = user();
        mvc.perform(get("/community/users")).andExpect(status().isUnauthorized());
        mvc.perform(get(path(target) + "/followers")).andExpect(status().isUnauthorized());
        mvc.perform(put(path(target) + "/following")).andExpect(status().isUnauthorized());
        mvc.perform(delete(path(target) + "/following")).andExpect(status().isUnauthorized());
    }

    @Test void repeatedFollowAndUnfollowAreIdempotentAndUseOnlySessionIdentity() throws Exception {
        User actor = user(), target = user(), bystander = user(); String token = auth(actor);
        for (int i = 0; i < 2; i++)
            mvc.perform(put(path(target) + "/following").header("Authorization", token)
                    .param("followerId", bystander.getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.following").value(true))
                    .andExpect(jsonPath("$.data.followerCount").value(1));
        assertEquals(1, follows.selectCountByCondition(USER_FOLLOW.FOLLOWER_ID.eq(actor.getId())));
        assertEquals(0, follows.selectCountByCondition(USER_FOLLOW.FOLLOWER_ID.eq(bystander.getId())));
        for (int i = 0; i < 2; i++)
            mvc.perform(delete(path(target) + "/following").header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.following").value(false))
                    .andExpect(jsonPath("$.data.followerCount").value(0));
    }

    @Test void selfFollowIsForbiddenAndSelfSummaryHasNoAction() throws Exception {
        User actor = user(); String token = auth(actor);
        mvc.perform(put(path(actor) + "/following").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get(path(actor) + "/relationship").header("Authorization", token))
                .andExpect(jsonPath("$.data.self").value(true)).andExpect(jsonPath("$.data.canFollow").value(false));
    }

    @Test void bothDirectionsAndMutualRelationsAreVisibleWithoutPrivateFields() throws Exception {
        User actor = user(), target = user(); edge(actor, target); edge(target, actor); String token = auth(actor);
        mvc.perform(get(path(target) + "/relationship").header("Authorization", token))
                .andExpect(jsonPath("$.data.following").value(true)).andExpect(jsonPath("$.data.followedBy").value(true))
                .andExpect(jsonPath("$.data.followingCount").value(1)).andExpect(jsonPath("$.data.followerCount").value(1));
        for (String direction : List.of("followers", "following"))
            mvc.perform(get(path(actor) + "/" + direction).header("Authorization", token))
                    .andExpect(jsonPath("$.data.totalRow").value(1))
                    .andExpect(jsonPath("$.data.records[0].id").value(target.getId()))
                    .andExpect(jsonPath("$.data.records[0].following").value(true))
                    .andExpect(jsonPath("$.data.records[0].followedBy").value(true))
                    .andExpect(jsonPath("$.data.records[0].password").doesNotExist())
                    .andExpect(jsonPath("$.data.records[0].email").doesNotExist())
                    .andExpect(jsonPath("$.data.records[0].phone").doesNotExist())
                    .andExpect(jsonPath("$.data.records[0].roleIds").doesNotExist());
    }

    @Test void deletedAccountsDisappearFromCountsAndBothLists() throws Exception {
        User actor = user(), removed = user(); edge(actor, removed); edge(removed, actor); users.deleteById(removed.getId());
        String token = auth(actor);
        mvc.perform(get(path(actor) + "/relationship").header("Authorization", token))
                .andExpect(jsonPath("$.data.followingCount").value(0)).andExpect(jsonPath("$.data.followerCount").value(0));
        for (String direction : List.of("followers", "following"))
            mvc.perform(get(path(actor) + "/" + direction).header("Authorization", token))
                    .andExpect(jsonPath("$.data.totalRow").value(0)).andExpect(jsonPath("$.data.records").isEmpty());
        mvc.perform(put(path(removed) + "/following").header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(get(path(removed) + "/followers").header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test void disabledTargetsCannotBeFollowedButExistingRelationsCanBeRemoved() throws Exception {
        User actor = user(), target = user(); edge(actor, target); users.update(User.create().setId(target.getId()).setStatus(1));
        String token = auth(actor);
        mvc.perform(put(path(target) + "/following").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(delete(path(target) + "/following").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.following").value(false));
    }

    @Test void paginationIsStableAndOutOfRangeReturnsTheLastPage() throws Exception {
        User actor = user(), first = user(), second = user(), third = user();
        edge(actor, first); edge(actor, second); edge(actor, third); String token = auth(actor);
        mvc.perform(get(path(actor) + "/following").param("current", "1").param("size", "2").header("Authorization", token))
                .andExpect(jsonPath("$.data.totalRow").value(3)).andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value(third.getId()));
        mvc.perform(get(path(actor) + "/following").param("current", "99").param("size", "2").header("Authorization", token))
                .andExpect(jsonPath("$.data.pageNumber").value(2)).andExpect(jsonPath("$.data.records[0].id").value(first.getId()));
    }

    @Test void discoverySearchesNidAndNicknameAndExcludesSelfDisabledAndDeleted() throws Exception {
        User actor = user(), target = user(), disabled = user(), deleted = user(); String marker = "community" + UUID.randomUUID().toString().replace("-", "");
        for (User u : List.of(actor, target, disabled, deleted)) users.update(User.create().setId(u.getId()).setUsername(marker));
        users.update(User.create().setId(disabled.getId()).setStatus(2)); users.deleteById(deleted.getId());
        String token = auth(actor);
        for (String keyword : List.of(marker, target.getNid()))
            mvc.perform(get("/community/users").param("keyword", keyword).header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalRow").value(1))
                    .andExpect(jsonPath("$.data.records[0].id").value(target.getId()));
    }

    @Test void invalidIdsPaginationAndLongSearchAreRejected() throws Exception {
        String token = auth(user());
        mvc.perform(get("/community/users/0/relationship").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/community/users").param("current", "0").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/community/users").param("size", "101").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/community/users").param("keyword", "a".repeat(51)).header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/community/users/9223372036854775807/relationship").header("Authorization", token)).andExpect(status().isNotFound());
    }
}
