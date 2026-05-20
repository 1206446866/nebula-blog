package com.nebula.start.config;

import com.nebula.start.security.JwtAccessDeniedHandler;
import com.nebula.start.security.JwtAuthenticationEntryPoint;
import com.nebula.start.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {


    private final CustomUserDetailsService userDetailsService;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 关闭 csrf
                .csrf(AbstractHttpConfigurer::disable) // 关闭 CSRF
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // 所有接口放行
                .exceptionHandling(exception -> exception
                        // 未登录
                        .authenticationEntryPoint(
                                jwtAuthenticationEntryPoint
                        )

                        // 无权限
                        .accessDeniedHandler(
                                jwtAccessDeniedHandler
                        )
                );
        return http.build();
    }

    // AuthenticationManager 推荐写法（DaoAuthenticationProvider 注入）
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}