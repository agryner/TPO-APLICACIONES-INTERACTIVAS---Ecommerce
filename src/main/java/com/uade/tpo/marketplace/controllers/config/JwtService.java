package com.uade.tpo.marketplace.controllers.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Value("${marketplace.jwt.clave}")
    private String clave;

    @Value("${marketplace.jwt.duracion-ms}")
    private long duracionMs;

    /**
     * Pre : el usuario ya autenticado.
     * Post: el token firmado, con su mail como subject, la hora de emision y
     *       el vencimiento.
     */
    public String generarToken(UserDetails usuario) {
        long ahora = System.currentTimeMillis();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + duracionMs))
                .signWith(getClave())
                .compact();
    }

    public String extraerUsuario(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public boolean esValido(String token, UserDetails usuario) {
        return extraerUsuario(token).equals(usuario.getUsername()) && !estaVencido(token);
    }

    private boolean estaVencido(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> queSaco) {
        return queSaco.apply(extraerTodos(token));
    }

    private Claims extraerTodos(String token) {
        return Jwts.parser()
                .verifyWith(getClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getClave() {
        return Keys.hmacShaKeyFor(clave.getBytes(StandardCharsets.UTF_8));
    }
}
