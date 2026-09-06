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

/**
 * Firma y verifica los tokens.
 *
 * Vive en controllers/config junto al resto de la seguridad, siguiendo la
 * estructura del ejemplo de la catedra. No es una regla del marketplace: no
 * sabe de productos ni de ordenes, sabe de criptografia. Si manana se cambiara
 * JWT por sesiones, esta carpeta se reescribe entera y service no se entera.
 *
 * Un JWT tiene tres partes separadas por puntos: header, payload y firma. Las
 * dos primeras son JSON en Base64 y se leen sin ninguna clave, asi que el token
 * no oculta nada: lo que garantiza es que nadie lo modifico. Por eso adentro va
 * el mail y la fecha de vencimiento, y nunca la contrasena.
 */
@Service
public class JwtService {

    @Value("${marketplace.jwt.clave}")
    private String clave;

    @Value("${marketplace.jwt.duracion-ms}")
    private long duracionMs;

    /**
     * Emite un token para un usuario ya autenticado.
     *
     * Lo llaman solo el login y el registro: es el unico momento en que el
     * servidor se compromete con una identidad.
     *
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

    /** El mail que viaja en el token, o sea de quien es. */
    /**
     * Pre : el token.
     * Post: el mail que viaja adentro. Verifica la firma antes de leerlo, asi
     *       que si el token fue tocado explota en vez de devolver algo en lo
     *       que no se puede confiar.
     */
    public String extraerUsuario(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Si el token es de este usuario y todavia no vencio.
     *
     * Que la firma sea valida ya se comprobo al parsearlo: si estuviera
     * adulterado, extraerClaim habria explotado antes de llegar aca.
     *
     * Pre : el token y el usuario contra el que compararlo.
     * Post: si el token es de ese usuario y todavia no vencio.
     */
    public boolean esValido(String token, UserDetails usuario) {
        return extraerUsuario(token).equals(usuario.getUsername()) && !estaVencido(token);
    }

    /**
     * Pre : el token.
     * Post: si la fecha de expiracion ya paso.
     */
    private boolean estaVencido(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Pre : el token y una funcion que dice que campo sacar.
     * Post: ese campo. Es el metodo generico del que salen extraerUsuario y
     *       estaVencido.
     */
    private <T> T extraerClaim(String token, Function<Claims, T> queSaco) {
        return queSaco.apply(extraerTodos(token));
    }

    /**
     * Abre el token verificando la firma.
     *
     * verifyWith es lo que hace que esto sea seguro: si el payload fue tocado,
     * la firma deja de coincidir y tira una excepcion en vez de devolver datos
     * en los que no se puede confiar.
     *
     * Pre : el token.
     * Post: todos los claims, despues de verificar la firma con verifyWith.
     *       Ahi es donde esto se vuelve seguro: si el payload no coincide con
     *       la firma, tira excepcion.
     */
    private Claims extraerTodos(String token) {
        return Jwts.parser()
                .verifyWith(getClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Pre : nada; usa la clave de application.properties.
     * Post: la clave secreta en el formato que espera HMAC.
     */
    private SecretKey getClave() {
        return Keys.hmacShaKeyFor(clave.getBytes(StandardCharsets.UTF_8));
    }
}
