package com.skillup.common.security;

import com.skillup.common.BranchResolutionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Local development security configuration.
 * All endpoints are open — no JWT required.
 * @PreAuthorize annotations are NOT enforced (method security disabled for local).
 * Active on profiles: "local" and "local-no-kafka".
 *
 * Pass X-Branch-ID header manually when testing branch-scoped APIs.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("local | local-no-kafka")
public class LocalSecurityConfig {

    private final BranchResolutionFilter branchResolutionFilter;

    @Bean
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterAfter(branchResolutionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
