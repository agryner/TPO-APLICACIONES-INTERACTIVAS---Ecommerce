package com.uade.tpo.marketplace.controllers.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

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
