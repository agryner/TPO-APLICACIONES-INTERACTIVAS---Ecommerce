package com.uade.tpo.marketplace.controllers.auth;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "El mail es obligatorio")
    private String mail;

    @NotBlank(message = "La contrasena es obligatoria")
    private String contrasena;
}
