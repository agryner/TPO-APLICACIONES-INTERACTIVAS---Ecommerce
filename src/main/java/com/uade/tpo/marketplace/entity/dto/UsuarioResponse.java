package com.uade.tpo.marketplace.entity.dto;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;

import lombok.Data;

/**
 * Vista publica de un usuario.
 *
 * Deja afuera la contrasena y las colecciones de productos, carritos y ordenes:
 * la entidad Usuario nunca sale de la capa de servicios. Lo arma el service y
 * el controller lo devuelve tal cual.
 */
@Data
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String mail;
    private String direccion;
    private TipoUsuario rol;

    /** false = dado de baja: no opera ni aparece en los listados. */
    private Boolean activo;

    public static UsuarioResponse from(Usuario usuario) {
        if (usuario == null)
            return null;

        UsuarioResponse dto = new UsuarioResponse();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setNombreUsuario(usuario.getNombreUsuario());
        dto.setMail(usuario.getMail());
        dto.setDireccion(usuario.getDireccion());
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.getActivo());
        return dto;
    }
}
