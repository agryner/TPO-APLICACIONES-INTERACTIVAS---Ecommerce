package com.uade.tpo.marketplace.controllers.envios;

import java.time.LocalDateTime;

import com.uade.tpo.marketplace.controllers.usuarios.UsuarioPublicoResponse;
import com.uade.tpo.marketplace.entity.DireccionEntrega;
import com.uade.tpo.marketplace.entity.Envio;
import com.uade.tpo.marketplace.entity.EstadoEnvio;
import com.uade.tpo.marketplace.entity.MetodoEntrega;

import lombok.Data;

@Data
public class EnvioResponse {
    private Long id;
    private Long idOrden;
    private EstadoEnvio estado;
    private MetodoEntrega metodoEntrega;
    private String numeroSeguimiento;
    private DireccionEntrega entrega;
    private UsuarioPublicoResponse comprador;
    private UsuarioPublicoResponse vendedor;
    private UsuarioPublicoResponse despachante;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaDespacho;
    private LocalDateTime fechaEntrega;

    /**
     * Pre : la entidad Envio, o null.
     * Post: el envio con las tres puntas en vista reducida. La direccion sale
     *       de la orden y si viaja: quien lo mueve la necesita. En las
     *       coordinadas viene vacia, porque la entrega la arreglan las partes.
     */
    public static EnvioResponse from(Envio envio) {
        if (envio == null)
            return null;

        EnvioResponse dto = new EnvioResponse();
        dto.setId(envio.getId());
        dto.setEstado(envio.getEstado());
        dto.setNumeroSeguimiento(envio.getNumeroSeguimiento());

        dto.setDespachante(UsuarioPublicoResponse.from(envio.getDespachante()));
        dto.setFechaCreacion(envio.getFechaCreacion());
        dto.setFechaDespacho(envio.getFechaDespacho());
        dto.setFechaEntrega(envio.getFechaEntrega());

        if (envio.getOrden() != null) {
            dto.setIdOrden(envio.getOrden().getId());
            dto.setMetodoEntrega(envio.getOrden().getMetodoEntrega());
            dto.setEntrega(envio.getOrden().getEntrega());
            dto.setComprador(UsuarioPublicoResponse.from(envio.getOrden().getComprador()));
            dto.setVendedor(UsuarioPublicoResponse.from(envio.getOrden().getVendedor()));
        }
        return dto;
    }
}
