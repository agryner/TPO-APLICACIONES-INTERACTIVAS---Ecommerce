package com.uade.tpo.marketplace.controllers.envios;

import java.time.LocalDateTime;
import java.util.List;

import com.uade.tpo.marketplace.entity.Envio;
import com.uade.tpo.marketplace.entity.Provincia;

import lombok.Data;

@Data
public class EntregaResponse {
    public record Llevado(String nombre, Integer cantidad) {}

    private String numeroSeguimiento;
    private LocalDateTime fechaEntrega;
    private Provincia provincia;
    private String localidad;
    private List<Llevado> productos;

    /**
     * Pre : la entidad Envio, o null.
     * Post: que numero se entrego, cuando, a que localidad y que llevaba. El
     *       nombre y la cantidad salen del renglon de la orden, que los guarda
     *       como se vendieron. La localidad y la provincia si, LA CALLE NO:
     *       quien entrego mil paquetes se quedaria con mil direcciones exactas,
     *       y para contar lo que hizo le alcanza con saber a que pueblo fue.
     *       Tampoco lleva comprador ni vendedor.
     */
    public static EntregaResponse from(Envio envio) {
        if (envio == null)
            return null;

        EntregaResponse dto = new EntregaResponse();
        dto.setNumeroSeguimiento(envio.getNumeroSeguimiento());
        dto.setFechaEntrega(envio.getFechaEntrega());
        if (envio.getOrden() != null && envio.getOrden().getEntrega() != null) {
            dto.setProvincia(envio.getOrden().getEntrega().getProvincia());
            dto.setLocalidad(envio.getOrden().getEntrega().getLocalidad());
        }

        dto.setProductos(envio.getOrden() == null
                ? List.of()
                : envio.getOrden().getItems().stream()
                        .map(i -> new Llevado(i.getNombre(), i.getCantidad()))
                        .toList());
        return dto;
    }
}
