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

/**
 * Que se puede pedir sin token y que no.
 *
 * Las reglas se evaluan en orden y gana la primera que coincide, asi que lo
 * especifico va antes que lo general. Por eso mis-publicaciones y todos estan
 * declarados arriba de /productos/*: si fuera al reves, el comodin los abriria
 * al publico.
 *
 * Lo que decide aca es solo si hace falta estar logueado. Quien puede hacer
 * cada cosa -si sos el duenio, si sos ADMIN- lo sigue decidiendo
 * AutorizacionService, porque son reglas que dependen del recurso y no de la
 * ruta: hasAuthority no sabe expresar "el duenio de este producto o un admin".
 */
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
                // CSRF protege formularios con sesion y cookies. Esto es una API
                // stateless que se consume con un header, asi que no aplica.
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(req -> req

                        // Cuando un controller tira una excepcion, Spring hace un
                        // dispatch interno a /error para armar la respuesta. Ese
                        // dispatch vuelve a pasar por esta cadena, y si no se lo
                        // deja pasar cae en el anyRequest de abajo: el resultado es
                        // que cualquier 400, 403 o 409 termina saliendo como 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // --- publico: entrar y darse de alta ---
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(POST, "/usuarios").permitAll()

                        // --- privado, aunque cuelgue de una ruta publica ---
                        // Van antes que los comodines de abajo o quedarian abiertos.
                        .requestMatchers(GET, "/productos/mis-publicaciones", "/productos/todos")
                        .authenticated()
                        .requestMatchers(GET, "/fotos/pendientes").authenticated()

                        // --- el catalogo se mira sin cuenta ---
                        .requestMatchers(GET, "/productos", "/productos/*").permitAll()
                        .requestMatchers(GET, "/categorias", "/categorias/*",
                                "/categorias/*/subcategorias")
                        .permitAll()
                        .requestMatchers(GET, "/fotos", "/fotos/*", "/fotos/*/contenido",
                                "/fotos/*/base64")
                        .permitAll()

                        // --- todo lo demas pide token ---
                        .anyRequest().authenticated())

                // Sin esto, un pedido sin token cae en el entry point por defecto,
                // que responde 403. Y 403 esta mal: significa "se quien sos y no
                // podes". Cuando no hay token el servidor no sabe quien es nadie,
                // y eso es 401.
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(SIN_TOKEN);
                }))

                // Sin sesion en el servidor: cada request se para sola con su
                // token. Es lo que permite que la API no guarde estado, y la razon
                // por la que el token lleva vencimiento.
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))

                .authenticationProvider(authenticationProvider)

                // El filtro va antes del de usuario y contrasena porque para
                // cuando ese corra, la identidad ya tiene que estar resuelta.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
