package com.uade.tpo.marketplace.controllers.usuarios;

import com.uade.tpo.marketplace.entity.Usuario;

import lombok.Data;

@Data
public class UsuarioPublicoResponse {
    private String nombre;
    private String apellido;
    private String nombreUsuario;

    /**
     * Pre : la entidad Usuario, o null.
     * Post: solo el nombre, el apellido y el nombre de usuario.
     */
    public static UsuarioPublicoResponse from(Usuario usuario) {
        if (usuario == null)
            return null;

        UsuarioPublicoResponse dto = new UsuarioPublicoResponse();
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setNombreUsuario(usuario.getNombreUsuario());
        return dto;
    }
}
