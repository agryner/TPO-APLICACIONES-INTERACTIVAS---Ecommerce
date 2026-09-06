package com.uade.tpo.marketplace.controllers.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * Lo que se manda para loguearse.
 *
 * El mail es lo que identifica al usuario, asi que es lo que viaja aca y lo que
 * despues queda como subject del token.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "El mail es obligatorio")
    private String mail;

    @NotBlank(message = "La contrasena es obligatoria")
    private String contrasena;
}
