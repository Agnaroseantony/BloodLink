package com.bloodlink.config;

import com.bloodlink.repository.AppUserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> users.findByUsername(username.toLowerCase(java.util.Locale.ROOT).trim())
            .map(user -> User.withUsername(user.getUsername()).password(user.getPasswordHash())
                .roles(user.getRole().name()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/vendor/**", "/error").permitAll()
                .requestMatchers("/hospital/**").hasRole("HOSPITAL")
                .requestMatchers("/donor/**", "/api/notifications/**").hasRole("DONOR")
                .anyRequest().authenticated())
            .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .headers(headers -> headers.referrerPolicy(policy -> policy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .build(); // CSRF stays enabled, including every state-changing form.
    }
}
