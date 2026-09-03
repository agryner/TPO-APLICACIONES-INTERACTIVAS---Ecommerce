package com.uade.tpo.marketplace.entity.dto;

import com.uade.tpo.marketplace.entity.TipoUsuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Datos que entran por el body al crear o editar un usuario.
 *
 * Spring lo arma desde el JSON y viaja del controller al service, que copia
 * sus campos a la entidad Usuario.
 *
 * El campo rol se ignora en el alta publica: UsuarioServiceImpl fuerza CLIENTE.
 * Queda declarado porque la edicion si lo usa, pero nadie se puede hacer
 * administrador mandandolo en el body.
 */
@Data
public class UsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no puede superar los 50 caracteres")
    private String apellido;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 30, message = "El nombre de usuario no puede superar los 30 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El mail es obligatorio")
    @Email(message = "El mail no tiene un formato valido")
    @Size(max = 100, message = "El mail no puede superar los 100 caracteres")
    private String mail;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(max = 100, message = "La contrasena no puede superar los 100 caracteres")
    private String contrasena;

    @Size(max = 150, message = "La direccion no puede superar los 150 caracteres")
    private String direccion;

    private TipoUsuario rol;
}
