package com.uade.tpo.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LimpiadorDeCarritos {
    private static final Logger log = LoggerFactory.getLogger(LimpiadorDeCarritos.class);

    private final CarritoService carritoService;

    /**
     * Pre : nada. La corre Spring sola cada marketplace.carrito.limpieza-ms.
     * Post: nada. Los carritos cuya fechaLimite ya paso quedan vacios, aunque
     *       su dueno no los mire. Solo deja rastro en el log cuando vacio
     *       alguno, para no escribir una linea por minuto sin motivo.
     */
    @Scheduled(fixedDelayString = "${marketplace.carrito.limpieza-ms:60000}")
    public void limpiar() {
        int vaciados = carritoService.vaciarVencidos();

        if (vaciados > 0)
            log.info("Se vaciaron {} carritos vencidos", vaciados);
    }
}
