package com.houra.jobinterviewcoach.config;

import com.houra.jobinterviewcoach.service.AiRequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private final AiRequestRateLimiter rateLimiter;

    public AiRateLimitInterceptor(AiRequestRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (!HttpMethod.POST.matches(request.getMethod())
                || rateLimiter.tryAcquire(request.getRemoteAddr())) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(rateLimiter.retryAfterSeconds()));
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("Too many AI requests. Please wait and try again.");
        return false;
    }
}
