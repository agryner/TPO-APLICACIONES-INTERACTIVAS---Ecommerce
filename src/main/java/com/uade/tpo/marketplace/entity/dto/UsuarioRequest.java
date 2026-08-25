package com.uade.tpo.marketplace.entity.dto;

import com.uade.tpo.marketplace.entity.TipoUsuario;

import lombok.Data;

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
