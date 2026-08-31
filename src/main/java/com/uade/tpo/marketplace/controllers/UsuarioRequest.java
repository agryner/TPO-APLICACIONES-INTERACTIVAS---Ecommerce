package com.uade.tpo.marketplace.controllers;

import com.uade.tpo.marketplace.entity.TipoUsuario;

import lombok.Data;

/**
 * Datos que entran por el body al crear o editar un usuario.
 *
 * Spring lo arma desde el JSON y viaja del controller al service, que copia
 * sus campos a la entidad Usuario.
 */
@Data
public class UsuarioRequest {
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String mail;
    private String contrasena;
    private String direccion;
    private TipoUsuario rol;
}
