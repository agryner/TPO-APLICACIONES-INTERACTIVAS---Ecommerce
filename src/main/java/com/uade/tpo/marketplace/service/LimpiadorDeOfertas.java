package com.uade.tpo.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LimpiadorDeOfertas {
    private static final Logger log = LoggerFactory.getLogger(LimpiadorDeOfertas.class);

    private final OfertaService ofertaService;

    /**
     * Pre : nada. La corre Spring sola cada marketplace.ofertas.limpieza-ms.
     * Post: nada. Las ofertas sin responder pasadas de fecha quedan VENCIDAS y
     *       el comprador se entera. Corre cada media hora: la vigencia es de
     *       dias, no hace falta mirar mas seguido.
     */
    @Scheduled(fixedDelayString = "${marketplace.ofertas.limpieza-ms:1800000}")
    public void limpiar() {
        int vencidas = ofertaService.vencerLasViejas();

        if (vencidas > 0)
            log.info("Vencieron {} ofertas sin responder", vencidas);
    }
}
