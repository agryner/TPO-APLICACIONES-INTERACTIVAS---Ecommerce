package com.uade.tpo.marketplace.controllers.mensajes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MensajeRequest {
    @NotBlank(message = "El mensaje no puede estar vacio")
    @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
    private String texto;
}
