package com.example.ControlNGR.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private JWTFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // IMPORTANTE: Permitir /error para ver excepciones reales (400, 500) en vez de 403
                .requestMatchers("/error").permitAll()
                
                // Recursos estáticos
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/img/**",
                    "/css/**",
                    "/js/**",
                    "/static/**",
                    "/resources/**",
                    "/uploads/**",
                    "/assets/**",
                    "/favicon.ico",
                    "/*.png",
                    "/*.jpg",
                    "/*.jpeg",
                    "/*.gif",
                    "/*.svg",
                    "/*.ico",
                    "/*.html",
                    "/webjars/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // Endpoints públicos de API
                .requestMatchers("/api/auth/**").permitAll()
                
             // VER empleados (Organigrama / Listado) - Todos los roles
                .requestMatchers(HttpMethod.GET, "/api/empleados/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "HD", "NOC")

                // VER horarios (consolidado, por empleado, exportar) - Todos los roles
                .requestMatchers(HttpMethod.GET, "/api/horarios/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "HD", "NOC")

                // Subir imágenes de perfil - Todos los usuarios autenticados
                .requestMatchers("/api/imagenes/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "HD", "NOC")

                // Endpoints protegidos por Rol (CRUD empleados y horarios - POST/PUT/DELETE)
                .requestMatchers("/api/empleados/**", "/api/horarios/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR")

                .requestMatchers("/api/asistencia/**", "/api/solicitudes/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "HD", "NOC")
                .requestMatchers("/api/solicitudes/exportar/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "TECNICO", "HD", "NOC")
                
                // El resto requiere autenticación
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://127.0.0.1:4200",
            "http://localhost:8080",
            "http://127.0.0.1:8080"
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*")); // Permitir todos los headers
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}