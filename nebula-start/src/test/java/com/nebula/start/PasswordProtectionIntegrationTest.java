package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.service.*;
import com.nebula.common.security.*;
import com.nebula.common.exception.ResourceLimitException;
import com.nebula.common.mail.service.MailService;
import com.nebula.user.entity.*;
import com.nebula.user.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordProtectionIntegrationTest extends JwtIntegrationTestSupport {
    static final String PREFIX="test:sensitive:"+UUID.randomUUID()+":";
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { r.add("nebula.security.sensitive-password.key-prefix",()->PREFIX); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper roles;
    @Autowired PasswordEncoder encoder;
    @Autowired SensitivePasswordVerifier verifier;
    @Autowired SensitivePasswordProperties limits;
    @Autowired StringRedisTemplate redis;
    @Autowired JwtUtil jwt;
    @Autowired AuthService auth;
    @MockitoBean MailService mail;
    @MockitoBean LoginLogService logs;
    List<String> tokens=new ArrayList<>();
    @AfterEach void clean() {
        SecurityContextHolder.clearContext(); tokens.forEach(jwt::revokeToken);
        var keys=redis.keys(PREFIX+"*"); if(keys!=null&&!keys.isEmpty())redis.delete(keys);
        limits.setIpRequests(30);
    }
    User fixture(long role) {
        var user=User.create().setNid("password_"+UUID.randomUUID()).setUsername("密码保护回归")
                .setPassword(encoder.encode("123")).setStatus(0).setDeleted(0);
        users.insert(user);roles.insert(UserRole.create().setUserId(user.getId()).setRoleId(role));return user;
    }
    String token(User user){String value=jwt.createToken(user);tokens.add(value);return value;}
    MvcResult change(String token,String old,String next,String ip)throws Exception{
        return mvc.perform(post("/auth/change-password").header("Authorization","Bearer "+token).with(r->{r.setRemoteAddr(ip);return r;})
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("oldPassword",old,"newPassword",next,"confirmPassword",next)))).andReturn();
    }
    MvcResult cancel(String token,String password)throws Exception{
        return mvc.perform(delete("/users/self").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("password",password,"confirmed",true)))).andReturn();
    }
    @Test void existingThreeCharacterPasswordCanLoginChangeAndOldTokensAreRevoked()throws Exception{
        var user=fixture(3L);String old=token(user);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("account",user.getNid(),"password","123"))))
                .andExpect(status().isOk()).andDo(result->tokens.add(json.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText()));
        assertEquals(200,change(old,"123","123456","192.0.2.1").getResponse().getStatus());
        assertTrue(encoder.matches("123456",users.selectOneById(user.getId()).getPassword()));
        mvc.perform(get("/users/"+user.getId()).header("Authorization","Bearer "+old)).andExpect(status().isUnauthorized());
    }
    @Test void oldShortPasswordCanConfirmCancellation()throws Exception{
        User user=fixture(3L);assertEquals(200,cancel(token(user),"123").getResponse().getStatus());assertNull(users.selectOneById(user.getId()));
    }
    @Test void adminWithOldShortPasswordCanSetSixCharacterPassword()throws Exception{
        User operator=fixture(1L),target=fixture(3L);
        mvc.perform(post("/auth/reset-password/"+target.getId()).header("Authorization","Bearer "+token(operator))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("operatorPassword","123","newPassword","123456","confirmPassword","123456"))))
                .andExpect(status().isOk());
        assertTrue(encoder.matches("123456",users.selectOneById(target.getId()).getPassword()));
    }
    @Test void passwordSpacesAreNotSilentlyRemoved()throws Exception{
        User owner=fixture(3L);assertEquals(200,change(token(owner),"123"," 1234 ","192.0.2.1").getResponse().getStatus());
        String hash=users.selectOneById(owner.getId()).getPassword();
        assertTrue(encoder.matches(" 1234 ",hash));assertFalse(encoder.matches("1234",hash));
    }
    @Test void shortNewPasswordIsRejectedBeforeOldPasswordAttemptAndServiceCannotBypassPolicy()throws Exception{
        var user=fixture(3L);String authToken=token(user);
        assertEquals(400,change(authToken,"wrong","12345","192.0.2.1").getResponse().getStatus());
        assertFalse(redis.hasKey(PREFIX+"account:"+user.getId()));
        var dto=new com.nebula.auth.dto.EmailVerifyDTO();dto.setPassword("12345");
        assertThrows(com.nebula.common.exception.BusinessException.class,()->auth.verifyEmailCaptcha(dto));
        org.mockito.Mockito.verifyNoInteractions(mail);
    }
    @Test void dtoRulesAreSharedAcrossRegistrationChangeAndResetButNotOldPasswords(){
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){
            var v=factory.getValidator();
            var register=new com.nebula.auth.dto.EmailVerifyDTO();register.setEmail("fixture@example.invalid");register.setCode("123456");register.setPassword("123456");
            var change=new com.nebula.auth.dto.ChangePasswordDTO();change.setOldPassword("1");change.setNewPassword("123456");change.setConfirmPassword("123456");
            var reset=new com.nebula.auth.dto.ResetPasswordDTO();reset.setOperatorPassword("1");reset.setNewPassword("123456");reset.setConfirmPassword("123456");
            assertTrue(v.validate(register).isEmpty());assertTrue(v.validate(change).isEmpty());assertTrue(v.validate(reset).isEmpty());
            register.setPassword("12345");change.setNewPassword("12345");reset.setNewPassword("12345");
            assertFalse(v.validate(register).isEmpty());assertFalse(v.validate(change).isEmpty());assertFalse(v.validate(reset).isEmpty());
        }
    }
    @Test void differentEndpointsIpsAndTokensShareOneOperatorBudget()throws Exception{
        User operator=fixture(1L), target=fixture(3L);String first=token(operator),second=token(operator);
        for(int i=0;i<3;i++)assertEquals(400,change(first,"wrong","123456","192.0.2."+(i+1)).getResponse().getStatus());
        for(int i=0;i<2;i++)assertEquals(400,cancel(second,"wrong").getResponse().getStatus());
        mvc.perform(post("/auth/reset-password/"+target.getId()).header("Authorization","Bearer "+first)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("operatorPassword","123","newPassword","123456","confirmPassword","123456"))))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        assertEquals(429,change(second,"123","123456","198.51.100.1").getResponse().getStatus());
    }
    @Test void successfulVerificationClearsFailuresAndExpiryRestoresAccess()throws Exception{
        User user=fixture(3L);String value=token(user);
        for(int i=0;i<4;i++)assertEquals(400,cancel(value,"wrong").getResponse().getStatus());
        assertEquals(200,change(value,"123","123456","192.0.2.1").getResponse().getStatus());
        assertFalse(redis.hasKey(PREFIX+"account:"+user.getId()));
        user=users.selectOneById(user.getId());value=token(user);
        for(int i=0;i<5;i++)assertEquals(400,cancel(value,"wrong").getResponse().getStatus());
        redis.expire(PREFIX+"account:"+user.getId(),Duration.ofMillis(20));Thread.sleep(40);
        assertEquals(200,cancel(value,"123456").getResponse().getStatus());
    }
    @Test void ipBudgetCannotBeResetBySuccessfulVerificationOrSwitchingAccounts()throws Exception{
        limits.setIpRequests(1);User first=fixture(3L),second=fixture(3L);
        assertEquals(200,change(token(first),"123","123456","192.0.2.1").getResponse().getStatus());
        assertEquals(429,change(token(second),"123","123456","192.0.2.1").getResponse().getStatus());
        assertEquals(200,change(token(second),"123","123456","192.0.2.2").getResponse().getStatus());
    }
    @Test void concurrentWrongAttemptsCannotExceedFive()throws Exception{
        User user=fixture(3L);var pool=Executors.newFixedThreadPool(10);var start=new CountDownLatch(1);
        try{
            List<Future<Integer>> results=new ArrayList<>();
            for(int i=0;i<10;i++)results.add(pool.submit(()->{var p=new com.nebula.auth.security.AuthLoginUser(user,List.of(),List.of());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));
                try{start.await();return verifier.matches("wrong",user.getPassword())?200:400;}catch(ResourceLimitException e){return e.getHttpStatus();}finally{SecurityContextHolder.clearContext();}}));
            start.countDown();List<Integer> statuses=new ArrayList<>();for(var r:results)statuses.add(r.get(15,TimeUnit.SECONDS));
            assertEquals(5,statuses.stream().filter(s->s==400).count());assertEquals(5,statuses.stream().filter(s->s==429).count());
        }finally{pool.shutdownNow();}
    }
}
