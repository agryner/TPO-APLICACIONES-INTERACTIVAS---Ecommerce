package com.uade.tpo.marketplace.controllers.usuarios;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;

import lombok.Data;

@Data
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String mail;
    private String direccion;
    private TipoUsuario rol;

    private Boolean activo;

    /**
     * Pre : la entidad Usuario, o null.
     * Post: los datos publicos. La contrasena no aparece porque este DTO ni
     *       siquiera tiene el campo: no es que se filtre, es que no existe.
     */
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
