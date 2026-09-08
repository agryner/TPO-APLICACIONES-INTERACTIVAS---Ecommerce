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

import jakarta.servlet.DispatcherType;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String SIN_TOKEN = """
            {"status":401,"error":"Unauthorized","message":"Hace falta iniciar sesion"}""";

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

                .authorizeHttpRequests(req -> req

                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(GET, "/productos/mis-publicaciones", "/productos/todos")
                        .authenticated()
                        .requestMatchers(GET, "/fotos/pendientes").authenticated()

                        .requestMatchers(GET, "/productos", "/productos/*",
                                "/productos/vendedor/*")
                        .permitAll()
                        .requestMatchers(GET, "/categorias", "/categorias/*",
                                "/categorias/*/subcategorias")
                        .permitAll()
                        .requestMatchers(GET, "/fotos", "/fotos/*", "/fotos/*/contenido",
                                "/fotos/*/base64")
                        .permitAll()

                        .anyRequest().authenticated())

                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(SIN_TOKEN);
                }))

                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))

                .authenticationProvider(authenticationProvider)

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
