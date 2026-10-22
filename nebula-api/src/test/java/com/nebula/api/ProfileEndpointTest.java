package com.nebula.api;

import com.nebula.api.controller.ApiController;
import com.nebula.api.facade.TagHomeFacade;
import com.nebula.api.facade.UserProfileFacade;
import com.nebula.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ProfileEndpointTest {
    @Test
    void invalidPaginationProducesAClientErrorWithoutLoadingTheProfile() throws Exception {
        UserProfileFacade profiles = mock(UserProfileFacade.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ApiController(profiles, mock(TagHomeFacade.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/profile/1").param("commentPage", "0"))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/api/profile/1").param("publicArticleSize", "101"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(profiles);
    }
}
