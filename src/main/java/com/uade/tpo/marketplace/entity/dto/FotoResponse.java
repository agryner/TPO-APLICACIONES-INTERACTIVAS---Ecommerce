package com.uade.tpo.marketplace.entity.dto;

import com.uade.tpo.marketplace.entity.EstadoVerificacion;
import com.uade.tpo.marketplace.entity.Foto;

import lombok.Data;

/**
 * Vista publica de una foto: solo los metadatos.
 *
 * Los bytes de la imagen no viajan aca; se piden en /fotos/{id}/contenido o en
 * /fotos/{id}/base64, que devuelve un FotoContenidoResponse.
 */
@Data
public class FotoResponse {

    private Long id;
    private String nombreArchivo;
    private String tipoContenido;
    private Long tamanio;
    private String url;

    /** APROBADA o EN_REVISION segun lo que dijo la verificacion automatica. */
    private EstadoVerificacion estadoVerificacion;

    private Double confianzaIa;

    private String queVeIa;

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
        return dto;
    }
}
