package com.uade.tpo.marketplace.controllers.fotos;

import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.Foto;

import lombok.Data;

@Data
public class FotoResponse {
    private Long id;
    private String nombreArchivo;
    private String tipoContenido;
    private Long tamanio;
    private String url;

    private EstadoVerificacion estadoVerificacion;

    private Double confianzaIa;

    private String queVeIa;

    // De que producto es y en que categoria esta: sin esto, quien revisa la
    // cola de pendientes ve una foto suelta y no puede juzgar si corresponde.
    private Long idProducto;
    private String nombreProducto;
    private String nombreCategoria;

    /**
     * Pre : la entidad Foto, o null.
     * Post: solo los metadatos, la URL del contenido y el producto al que
     *       pertenece. Los bytes NUNCA salen por aca: para eso estan los
     *       endpoints de contenido y base64.
     */
    public static FotoResponse from(Foto foto) {
        if (foto == null)
            return null;

        FotoResponse dto = new FotoResponse();
        dto.setId(foto.getId());
        dto.setNombreArchivo(foto.getNombreArchivo());
        dto.setTipoContenido(foto.getTipoContenido());
        dto.setTamanio(foto.getTamanio());
        dto.setUrl("/fotos/" + foto.getId() + "/contenido");
        dto.setEstadoVerificacion(foto.getEstadoVerificacion());
        dto.setConfianzaIa(foto.getConfianzaIa());
        dto.setQueVeIa(foto.getQueVeIa());
        if (foto.getProducto() != null) {
            dto.setIdProducto(foto.getProducto().getId());
            dto.setNombreProducto(foto.getProducto().getNombre());
            if (foto.getProducto().getCategoria() != null)
                dto.setNombreCategoria(foto.getProducto().getCategoria().getNombre());
        }
        return dto;
    }
}
