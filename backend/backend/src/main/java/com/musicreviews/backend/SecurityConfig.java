package com.musicreviews.backend;

import com.musicreviews.backend.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // localhost:5173 -> Vite dev server (npm run dev)
        // localhost      -> nginx en Docker (puerto 80, sin sufijo)
        // zentimes.es    -> producción
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://localhost:5176",
            "http://localhost",
            "https://zentimes.es",
            "https://www.zentimes.es"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas: registro, login y consultas de catálogo
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/artistas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/albumes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/resenas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/estadisticas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/publico").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/username/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*/seguidores").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*/siguiendo").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*/contadores").permitAll()
                // Solo ADMIN: gestión del catálogo, lista de usuarios e importación desde Spotify
                .requestMatchers(HttpMethod.POST, "/api/artistas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/artistas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/artistas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/albumes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/albumes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/albumes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN") // listar todos: solo ADMIN (expone emails)
                .requestMatchers(HttpMethod.PATCH, "/api/usuarios/**").hasRole("ADMIN") // activar/desactivar cuentas
                .requestMatchers("/api/spotify/**").hasRole("ADMIN")
                // Resto de rutas: usuario autenticado
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autenticado"))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
