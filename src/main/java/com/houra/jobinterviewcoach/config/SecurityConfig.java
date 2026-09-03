package com.houra.jobinterviewcoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/", "/style.css", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.POST, "/questions", "/feedback").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/history", "/history/**").authenticated()
                        .anyRequest().denyAll()
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/history", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String password,
            PasswordEncoder passwordEncoder
    ) {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("APP_ADMIN_USERNAME and APP_ADMIN_PASSWORD must not be blank.");
        }

        UserDetails admin = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
