package com.nebula.api;

import com.nebula.common.exception.GlobalExceptionHandler;
import com.nebula.user.controller.UserController;
import com.nebula.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AccountCancellationEndpointTest {
    @Test
    void requiresPasswordAndExplicitConfirmationBeforeCallingTheService() throws Exception {
        var users = mock(UserService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UserController(users))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String body : new String[]{"{}", "{\"password\":\" \u0020\",\"confirmed\":true}",
                "{\"password\":\"current-password\",\"confirmed\":false}",
                "{\"password\":\"current-password\"}"}) {
            mvc.perform(delete("/users/self").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(jsonPath("$.code").value(400));
        }
        verifyNoInteractions(users);
        when(users.cancelCurrentAccount(any())).thenReturn(true);
        mvc.perform(delete("/users/self").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"current-password\",\"confirmed\":true}"))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").value(true));
        verify(users).cancelCurrentAccount(any());
    }
}
