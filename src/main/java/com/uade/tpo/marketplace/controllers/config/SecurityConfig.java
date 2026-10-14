package com.uade.tpo.marketplace.controllers.config;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.DispatcherType;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String SIN_TOKEN = """
            {"timestamp":"%s","status":401,"error":"Unauthorized",            "message":"Hace falta iniciar sesion"}""";

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * Pre : el HttpSecurity que arma Spring.
     * Post: la cadena de filtros configurada: sin CSRF, sin sesion, con el
     *       filtro de JWT antes del de usuario y contrasena, las rutas
     *       publicas declaradas y un 401 -no un 403- cuando falta el token.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(req -> req

                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(GET, "/productos/mis-publicaciones", "/productos/todos")
                        .authenticated()
                        .requestMatchers(GET, "/fotos/pendientes").authenticated()

                        .requestMatchers(GET, "/productos", "/productos/*",
                                "/productos/vendedor/*", "/productos/*/similares")
                        .permitAll()
                        .requestMatchers(GET, "/categorias/inactivas").authenticated()
                        .requestMatchers(GET, "/categorias", "/categorias/*",
                                "/categorias/*/subcategorias")
                        .permitAll()
                        .requestMatchers(GET, "/fotos", "/fotos/*", "/fotos/*/contenido",
                                "/fotos/*/base64")
                        .permitAll()
                        .requestMatchers(GET, "/resenas/producto/*", "/resenas/vendedor/*")
                        .permitAll()
                        .requestMatchers(GET, "/destacados/planes").permitAll()

                        .anyRequest().authenticated())

                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(SIN_TOKEN.formatted(Instant.now()));
                }))

                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))

                .authenticationProvider(authenticationProvider)

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Pre : nada.
     * Post: los origenes del frontend en desarrollo (Vite, puerto 5173)
     *       habilitados para llamar a cualquier ruta, con el header
     *       Authorization incluido. Sin esto el navegador corta los pedidos
     *       antes de que lleguen al filtro de JWT.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
