package com.uade.tpo.marketplace.controllers.productos;

import com.uade.tpo.marketplace.entity.NivelVendedor;
import com.uade.tpo.marketplace.entity.Usuario;

import lombok.Data;

@Data
public class VendedorResponse {
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private NivelVendedor nivel;
    private Double calificacion;
    private long cantidadResenas;

    /**
     * Pre : el vendedor, su nivel y su calificacion general.
     * Post: el cuadro de "vendido por". Es un DTO propio y no el
     *       UsuarioPublicoResponse de siempre porque ese se usa en otros seis
     *       lugares donde un nivel de vendedor no significa nada: en una orden
     *       describe tambien al comprador. Sigue sin llevar mail, direccion, id
     *       ni rol: al vendedor se lo busca por nombre de usuario.
     */
    public static VendedorResponse from(Usuario vendedor, NivelVendedor nivel,
            Double calificacion, long cantidadResenas) {
        if (vendedor == null)
            return null;

        VendedorResponse dto = new VendedorResponse();
        dto.setNombre(vendedor.getNombre());
        dto.setApellido(vendedor.getApellido());
        dto.setNombreUsuario(vendedor.getNombreUsuario());
        dto.setNivel(nivel);
        dto.setCalificacion(calificacion);
        dto.setCantidadResenas(cantidadResenas);
        return dto;
    }
}
