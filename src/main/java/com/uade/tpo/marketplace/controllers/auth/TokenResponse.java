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
 */
@Data
public class TokenResponse {

    @JsonProperty("access_token")
    private String token;

    /**
     * Pre : el token ya firmado.
     * Post: el DTO con ese unico campo, serializado como access_token.
     */
    public static TokenResponse from(String token) {
        TokenResponse dto = new TokenResponse();
        dto.setToken(token);
        return dto;
    }
}
