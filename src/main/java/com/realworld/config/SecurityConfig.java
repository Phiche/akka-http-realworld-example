package com.realworld.config;

import com.realworld.security.AuthEntryPointJwt;
import com.realworld.security.JwtAuthTokenFilter;
import com.realworld.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // To enable method-level security like @PreAuthorize
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private JwtAuthTokenFilter jwtAuthTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // This method of configuring AuthenticationManagerBuilder is less common in Spring Boot 3+
    // It's usually auto-configured if UserDetailsService and PasswordEncoder beans are available.
    // protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    //     auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    // }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {}) // Enable CORS, further configuration might be needed via CorsConfigurationSource bean
            .csrf(csrf -> csrf.disable()) // Disable CSRF as we are using JWT
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (RealWorld specific)
                .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/profiles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/{slug}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/{slug}/comments").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tags").permitAll()

                // Endpoints requiring authentication
                .requestMatchers("/api/user/**").authenticated()
                .requestMatchers("/api/profiles/{username}/follow").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/articles").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/articles/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/articles/**").authenticated()
                .requestMatchers("/api/articles/feed").authenticated()
                .requestMatchers("/api/articles/{slug}/favorite").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/articles/{slug}/comments").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/articles/{slug}/comments/{id}").authenticated()

                // Any other request must be authenticated (default fall-through)
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
