package com.nebula.start;

import com.mybatisflex.core.row.Db;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserFollowMapper;
import com.nebula.user.service.UserFollowService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.nebula.user.entity.table.UserFollowTableDef.USER_FOLLOW;

/** 使用专用已提交账号验证真实并发锁；结束时仅物理清理本测试创建的ID。 */
@SpringBootTest
@AutoConfigureMockMvc
class UserFollowConcurrencyIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired UserMapper users;
    @Autowired UserFollowMapper follows;
    @Autowired UserFollowService service;
    final List<Long> fixtureIds = new ArrayList<>();
    final ExecutorService workers = Executors.newFixedThreadPool(4);

    private User user() {
        User user = User.create().setNid("follow_race_" + UUID.randomUUID().toString().replace("-", ""))
                .setUsername("关注并发测试").setPassword("unused").setStatus(0).setDeleted(0);
        users.insert(user); fixtureIds.add(user.getId()); return user;
    }
    private void as(User user, Runnable action) {
        var principal = new AuthLoginUser(user, List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        try { action.run(); } finally { SecurityContextHolder.clearContext(); }
    }
    @AfterEach void clean() throws Exception {
        workers.shutdownNow(); assertTrue(workers.awaitTermination(20, TimeUnit.SECONDS));
        for (Long id : fixtureIds) Db.deleteBySql("DELETE FROM user_follow WHERE follower_id=? OR following_id=?", id, id);
        for (Long id : fixtureIds) Db.deleteBySql("DELETE FROM user WHERE id=? AND nid LIKE 'follow_race_%'", id);
        SecurityContextHolder.clearContext();
    }

    @Test void concurrentRetriesCreateOneRelationshipAndConcurrentUnfollowsRemoveIt() throws Exception {
        User actor = user(), target = user();
        for (boolean follow : List.of(true, false)) {
            CountDownLatch ready = new CountDownLatch(4), release = new CountDownLatch(1);
            List<Future<?>> jobs = new ArrayList<>();
            for (int i = 0; i < 4; i++) jobs.add(workers.submit(() -> {
                ready.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("gate timed out"); }
                catch (InterruptedException e) { throw new RuntimeException(e); }
                as(actor, () -> service.setFollowing(target.getId(), follow));
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); release.countDown();
            for (var job : jobs) job.get(15, TimeUnit.SECONDS);
            assertEquals(follow ? 1 : 0, follows.selectCountByCondition(USER_FOLLOW.FOLLOWER_ID.eq(actor.getId())
                    .and(USER_FOLLOW.FOLLOWING_ID.eq(target.getId()))));
        }
    }

    @Test void simultaneousMutualFollowDoesNotDeadlockOrLoseEitherDirection() throws Exception {
        User first = user(), second = user(); CountDownLatch release = new CountDownLatch(1);
        List<Future<?>> jobs = new ArrayList<>();
        for (User actor : List.of(first, second)) jobs.add(workers.submit(() -> {
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("gate timed out"); }
            catch (InterruptedException e) { throw new RuntimeException(e); }
            as(actor, () -> service.setFollowing(actor == first ? second.getId() : first.getId(), true));
        }));
        release.countDown(); for (var job : jobs) job.get(15, TimeUnit.SECONDS);
        as(first, () -> {
            var result = service.summary(second.getId());
            assertTrue(result.following()); assertTrue(result.followedBy());
            assertEquals(1, result.followerCount()); assertEquals(1, result.followingCount());
        });
    }
}
