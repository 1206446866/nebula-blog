package com.nebula.api.facade;

import com.mybatisflex.core.paginate.Page;
import com.nebula.api.dto.ProfileQueryDto;
import com.nebula.api.vo.profile.UserProfileVO;
import com.nebula.article.entity.Article;
import com.nebula.article.service.ArticleService;
import com.nebula.auth.entity.LoginLog;
import com.nebula.auth.service.LoginLogService;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.service.CommentService;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.LoginUser;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserProfileFacadeTest {
    private final UserService users = mock(UserService.class);
    private final RoleService roles = mock(RoleService.class);
    private final ArticleService articles = mock(ArticleService.class);
    private final CommentService comments = mock(CommentService.class);
    private final LoginLogService logs = mock(LoginLogService.class);
    private final UserProfileFacade facade = new UserProfileFacade(users, roles, articles, comments, logs, Runnable::run);
    private ProfileQueryDto query;

    @BeforeEach
    void setUp() {
        query = new ProfileQueryDto();
        query.setPublicArticleSize(5);
        query.setDraftArticleSize(5);
        query.setCommentSize(5);
        when(users.getUserById(1L)).thenReturn(User.create().setId(1L).setUsername("Nebula")
                .setEmail("private@example.test").setPhone("private-phone"));
        when(roles.getDescriptions(1L)).thenReturn(List.of("用户"));
        when(articles.getViewAllCount(1L)).thenReturn(12L);
        when(articles.getLikeAllCount(1L)).thenReturn(3L);
        when(comments.getLikeAllCount(1L)).thenReturn(2L);
        when(articles.pageArticleProfile(eq(1L), anyInt(), anyInt(), eq(5)))
                .thenAnswer(invocation -> Page.of(invocation.<Integer>getArgument(2), 5, 0));
        when(comments.getCommentPageByUserId(eq(1L), anyInt(), eq(5)))
                .thenAnswer(invocation -> Page.of(invocation.<Integer>getArgument(1), 5, 0));
        when(comments.getCountByArticleIds(anyList())).thenReturn(Map.of());
        when(logs.getLoginLog(1L)).thenReturn(List.of(LoginLog.create().setId(10L).setUserId(1L).setStatus(1)));
    }

    private void authenticate(long id) {
        LoginUser principal = mock(LoginUser.class);
        when(principal.getUserId()).thenReturn(id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCanSeePrivateDetailsAndDrafts() {
        authenticate(1L);
        when(articles.pageArticleProfile(1L, 0, 1, 5)).thenReturn(
                new Page<>(List.of(Article.create().setId(100L).setTitle("Private draft")), 1, 5, 1));
        UserProfileVO result = facade.getProfile(1L, query);
        assertEquals("private@example.test", result.getUserProfileInfoVO().getEmail());
        assertEquals(1, result.getDraftArticles().getTotalRow());
        assertEquals(1, result.getLoginLogs().size());
        assertEquals(5L, result.getStatisticsVO().getLikeCount());
    }

    @Test
    void otherViewersIncludingAdminsNeverReceivePrivateDetails() {
        authenticate(2L);
        UserProfileVO result = facade.getProfile(1L, query);
        assertNull(result.getDraftArticles());
        assertNull(result.getUserProfileInfoVO().getEmail());
        assertNull(result.getUserProfileInfoVO().getPhone());
        assertTrue(result.getLoginLogs().isEmpty());
        verify(articles, never()).pageArticleProfile(eq(1L), eq(0), anyInt(), anyInt());
        verifyNoInteractions(logs);
        assertEquals("private@example.test", users.getUserById(1L).getEmail());
    }

    @Test
    void missingUserProducesBusinessErrorBeforeLoadingOtherData() {
        authenticate(1L);
        BusinessException error = assertThrows(BusinessException.class, () -> facade.getProfile(999L, query));
        assertEquals(40101, error.getCode());
        verifyNoInteractions(roles, articles, comments, logs);
    }

    @Test
    void oversizedPagesReturnTheLastExistingPage() {
        authenticate(1L);
        query.setPublicArticlePage(99);
        query.setCommentPage(99);
        query.setDraftArticlePage(99);
        when(articles.pageArticleProfile(eq(1L), anyInt(), anyInt(), eq(5)))
                .thenAnswer(invocation -> new Page<>(List.of(), invocation.<Integer>getArgument(2), 5, 6));
        when(comments.getCommentPageByUserId(eq(1L), anyInt(), eq(5)))
                .thenAnswer(invocation -> new Page<>(List.of(), invocation.<Integer>getArgument(1), 5, 11));
        UserProfileVO result = facade.getProfile(1L, query);
        assertEquals(2, result.getPublicArticles().getPageNumber());
        assertEquals(2, result.getDraftArticles().getPageNumber());
        assertEquals(3, result.getComments().getPageNumber());
        assertEquals(6L, result.getStatisticsVO().getArticleCount());
        assertEquals(11L, result.getStatisticsVO().getCommentCount());
    }

    @Test
    void emptyPagesResetToOneAndDoNotLoadAllArticleTitles() {
        authenticate(1L);
        query.setPublicArticlePage(99);
        query.setCommentPage(99);
        UserProfileVO result = facade.getProfile(1L, query);
        assertEquals(1, result.getPublicArticles().getPageNumber());
        assertEquals(1, result.getComments().getPageNumber());
        assertTrue(result.getComments().getRecords().isEmpty());
        verify(articles, never()).getArticleTitlesByCommentIds(anyList());
    }
}
