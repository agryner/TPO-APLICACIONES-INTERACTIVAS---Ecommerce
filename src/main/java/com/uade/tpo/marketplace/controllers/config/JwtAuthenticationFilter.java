package com.uade.tpo.marketplace.controllers.config;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

/**
 * Traduce el header Authorization en una identidad, en cada request.
 *
 * Corre antes que cualquier controller. Si hay un token valido deja al usuario
 * en el SecurityContext, y a partir de ahi todo el resto de la aplicacion puede
 * preguntar quien esta pidiendo sin que nadie lo mande como parametro.
 *
 * Si no hay token, o esta vencido, o la firma no cierra, no rechaza nada: deja
 * el contexto vacio y sigue. Quien decide si eso alcanza es SecurityConfig, no
 * este filtro. Esa division es lo que permite que convivan rutas publicas y
 * privadas sin escribir un if aca adentro.
 *
 * Extiende OncePerRequestFilter para no ejecutarse dos veces cuando el request
 * se despacha internamente, por ejemplo al resolver un error.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Pre : el request, el response y el resto de la cadena de filtros.
     * Post: si el header Authorization trae un token valido, el Usuario queda
     *       en el SecurityContext y de ahi lo lee todo el resto. Si no hay
     *       token, esta vencido o la firma no cierra, el contexto queda vacio
     *       y la cadena sigue igual: quien decide si eso alcanza es
     *       SecurityConfig, no este filtro.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(PREFIJO)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(PREFIJO.length());
            String mail = jwtService.extraerUsuario(token);

            // Si ya hay alguien autenticado no se pisa: otro filtro pudo haberlo
            // resuelto antes por otra via.
            if (mail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = userDetailsService.loadUserByUsername(mail);

                if (jwtService.esValido(token, usuario) && usuario.isEnabled()) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // Un token roto, vencido o de un usuario que ya no existe no es un
            // error del servidor: simplemente no autentica. Se sigue sin
            // identidad y responde 401 o 403 quien corresponda.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
