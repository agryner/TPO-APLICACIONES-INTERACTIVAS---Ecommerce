package com.uade.tpo.marketplace.controllers.envios;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.entity.Envio;

import lombok.Data;

@Data
public class EntregaResponse {
    private String numeroSeguimiento;
    private LocalDateTime fechaEntrega;

    /**
     * Pre : la entidad Envio, o null.
     * Post: solo que numero se entrego y cuando. Sin comprador, sin vendedor y
     *       sin direccion: el historial es del despachante, no de la gente por
     *       la que paso. Para contar lo que hizo y cuando le alcanza con esto.
     */
    public static EntregaResponse from(Envio envio) {
        if (envio == null)
            return null;

        EntregaResponse dto = new EntregaResponse();
        dto.setNumeroSeguimiento(envio.getNumeroSeguimiento());
        dto.setFechaEntrega(envio.getFechaEntrega());
        return dto;
    }
}
