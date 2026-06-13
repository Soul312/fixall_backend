package com.fixall.backend.config;

import com.fixall.backend.repository.UserRepository;
import com.fixall.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})  // Enable CORS with default config
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public routes — no token needed
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // Everything else (including /api/auth/me, /api/auth/fcm-token) requires a valid JWT
                .anyRequest().authenticated()
            )
            // Return structured JSON for auth errors instead of blank pages
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    writeJsonError(response, HttpStatus.UNAUTHORIZED,
                        "Authentication required — please log in. Your session may have expired.");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    writeJsonError(response, HttpStatus.FORBIDDEN,
                        "Access denied — you do not have permission to perform this action.");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Writes a structured JSON error response matching the GlobalExceptionHandler format.
     */
    private static void writeJsonError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = "{"
            + "\"timestamp\":\"" + LocalDateTime.now() + "\","
            + "\"status\":" + status.value() + ","
            + "\"error\":\"" + escapeJson(status.getReasonPhrase()) + "\","
            + "\"message\":\"" + escapeJson(message) + "\""
            + "}";

        response.getWriter().write(json);
    }

    /** Minimal JSON string escaping for the few static error messages above. */
    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
            .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));
    }

    // ── JWT Filter (inner class for simplicity) ─────────────────
    @Component
    @RequiredArgsConstructor
    public static class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtService jwtService;
        private final UserRepository userRepository;

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.startsWith("/uploads/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                HttpServletResponse response, FilterChain chain)
                throws IOException, jakarta.servlet.ServletException {

            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                chain.doFilter(request, response);
                return;
            }

            String token  = header.substring(7);
            String userId = jwtService.extractUserId(token);

            if (userId != null && jwtService.isTokenValid(token)) {
                userRepository.findById(userId).ifPresent(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                    org.springframework.security.core.context.SecurityContextHolder
                        .getContext().setAuthentication(auth);
                });
            }
            chain.doFilter(request, response);
        }
    }
}

