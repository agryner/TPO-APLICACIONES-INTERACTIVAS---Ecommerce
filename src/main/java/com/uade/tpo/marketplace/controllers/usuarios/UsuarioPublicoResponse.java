package com.uade.tpo.marketplace.controllers.usuarios;

import com.uade.tpo.marketplace.entity.Usuario;

import lombok.Data;

/**
 * Un usuario visto por otro: solo como se llama.
 *
 * UsuarioResponse trae el mail, la direccion, el rol, el id y el flag activo, y
 * eso es correcto donde se usa: la cuenta propia en /usuarios/me y los
 * endpoints de ADMIN. El problema aparecia cuando ese mismo DTO viajaba anidado
 * dentro de un producto o de una orden, porque el catalogo es publico: cualquier
 * visitante sin cuenta podia recorrerlo y armarse la lista de mails y domicilios
 * de todos los vendedores. Cerrar GET /usuarios/{id} no servia de nada mientras
 * la misma informacion saliera por la puerta de al lado.
 *
 * Aca no hay ningun campo que filtrar: los que no tienen que salir directamente
 * no existen. Tampoco viaja el id, y no hace falta, porque a un vendedor se lo
 * busca por nombre de usuario: GET /productos/vendedor/{nombreUsuario}.
 */
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
