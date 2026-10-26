package com.synthetic.readiness;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("lead").password(encoder.encode("demo-lead")).roles("READINESS_LEAD").build(),
            User.withUsername("ops").password(encoder.encode("demo-ops")).roles("TEAM_OWNER").build(),
            User.withUsername("uat").password(encoder.encode("demo-uat")).roles("UAT_LEAD").build(),
            User.withUsername("control").password(encoder.encode("demo-control")).roles("CONTROL_PARTNER").build(),
            User.withUsername("product").password(encoder.encode("demo-product")).roles("PRODUCT_PARTNER").build(),
            User.withUsername("viewer").password(encoder.encode("demo-viewer")).roles("EXECUTIVE_VIEWER").build());
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(a -> a.requestMatchers("/", "/css/**", "/actuator/health/**", "/api/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/decision/**").hasRole("READINESS_LEAD")
                .requestMatchers("/controls/**").hasRole("CONTROL_PARTNER")
                .requestMatchers("/actions/**").hasAnyRole("TEAM_OWNER", "READINESS_LEAD", "PRODUCT_PARTNER")
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults()).httpBasic(Customizer.withDefaults()).logout(Customizer.withDefaults()).build();
    }
}

