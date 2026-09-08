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
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
