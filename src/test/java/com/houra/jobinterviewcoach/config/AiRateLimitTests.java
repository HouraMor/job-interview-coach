package com.houra.jobinterviewcoach.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "app.rate-limit.request-limit=2")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiRateLimitTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void thirdAiRequestReturns429AndForwardedHeadersCannotEvadeTheLimit() throws Exception {
        mockMvc.perform(blankQuestionRequest("198.51.100.1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        mockMvc.perform(blankQuestionRequest("198.51.100.2"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        mockMvc.perform(blankQuestionRequest("198.51.100.3"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(content().string(containsString("Too many AI requests")));
    }

    private RequestBuilder blankQuestionRequest(String forwardedAddress) {
        return multipart("/questions")
                .header("X-Forwarded-For", forwardedAddress)
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr("192.0.2.20");
                    return request;
                });
    }
}
