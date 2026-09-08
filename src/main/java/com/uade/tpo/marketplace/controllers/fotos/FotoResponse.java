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

    /**
     * Pre : la entidad Foto, o null.
     * Post: solo los metadatos y la URL del contenido. Los bytes NUNCA salen
     *       por aca: para eso estan los endpoints de contenido y base64.
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
        return dto;
    }
}
