package com.uade.tpo.marketplace.controllers.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Lo que devuelven el login y el registro: el token y nada mas.
 *
 * Sale como access_token y no como token, para llamarse igual que en el ejemplo
 * de la catedra. El nombre viene de OAuth 2.0, donde el token de acceso convive
 * con un refresh_token; aca no hay refresh, asi que es solo la convencion del
 * nombre.
 *
 * El token no se guarda en la base: el servidor no lleva registro de los
 * emitidos. Que sea valido se decide verificando la firma y la fecha, no
 * buscandolo en ningun lado, y esa es la razon por la que no se puede invalidar
 * uno antes de que venza.
 *
 * Quien necesite saber quien es el usuario o que rol tiene lo saca del propio
 * token: el payload es Base64 y se lee sin ninguna clave.
 */
@Data
public class TokenResponse {

    @JsonProperty("access_token")
    private String token;

    /**
     * Envuelve el token en el objeto que sale por HTTP.
     *
     * Pre : el token ya firmado.
     * Post: el DTO con ese unico campo, serializado como access_token.
     */
    public static TokenResponse from(String token) {
        TokenResponse dto = new TokenResponse();
        dto.setToken(token);
        return dto;
    }
}
