package com.houra.jobinterviewcoach.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageRemainsPublicAndContainsACsrfToken() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void historyOverviewRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void historyDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/history/42"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedUserCanAccessHistory() throws Exception {
        mockMvc.perform(get("/history").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("history"));
    }

    @Test
    void configuredOwnerCredentialsCanLogIn() throws Exception {
        mockMvc.perform(formLogin().user("test-admin").password("test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/history"))
                .andExpect(authenticated().withUsername("test-admin"));
    }

    @Test
    void deleteRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/history/42/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void deleteRejectsMissingCsrfToken() throws Exception {
        mockMvc.perform(post("/history/42/delete").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
