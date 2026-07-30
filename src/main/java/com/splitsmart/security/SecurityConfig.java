package com.splitsmart.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // public API
                .requestMatchers("/users/register").permitAll()

                // protected APIs
                .requestMatchers("/users/**").authenticated()
                .requestMatchers("/groups/**").authenticated()
                .requestMatchers("/expenses/**").authenticated()

                .anyRequest().authenticated()
            )
            .httpBasic(); // 
        return http.build();
    }
}