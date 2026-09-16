package com.uade.tpo.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LimpiadorDeDestacadosImpl implements LimpiadorDeDestacados {
    private static final Logger log = LoggerFactory.getLogger(LimpiadorDeDestacados.class);

    private final DestacadoService destacadoService;

    /**
     * Pre : nada. La corre Spring sola cada marketplace.destacados.limpieza-ms.
     * Post: nada. Los destacados pasados de fecha vuelven a NINGUNO. Corre una
     *       vez por hora: la visibilidad se vende por meses, y el orden del
     *       catalogo ya ignora los vencidos mirando la fecha, asi que esto
     *       solo mantiene la columna honesta.
     */
    @Scheduled(fixedDelayString = "${marketplace.destacados.limpieza-ms:3600000}")
    public void limpiar() {
        int bajados = destacadoService.bajarLosVencidos();

        if (bajados > 0)
            log.info("Se dieron de baja {} destacados vencidos", bajados);
    }
}
