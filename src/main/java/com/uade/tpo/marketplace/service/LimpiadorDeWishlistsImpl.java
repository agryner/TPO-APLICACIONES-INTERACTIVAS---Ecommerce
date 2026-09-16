package com.uade.tpo.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LimpiadorDeWishlistsImpl implements LimpiadorDeWishlists {
    private static final Logger log = LoggerFactory.getLogger(LimpiadorDeWishlists.class);

    private final WishlistService wishlistService;

    /**
     * Pre : nada. La corre Spring sola cada marketplace.wishlist.limpieza-ms.
     * Post: nada. Las wishlists cuya fechaLimite ya paso quedan vacias, aunque
     *       su dueno no las mire. Corre mas espaciada que la del carrito
     *       porque la vigencia es de meses y no de dias: barrer seguido seria
     *       preguntar por algo que casi nunca cambio.
     */
    @Scheduled(fixedDelayString = "${marketplace.wishlist.limpieza-ms:3600000}")
    public void limpiar() {
        int vaciadas = wishlistService.vaciarVencidas();

        if (vaciadas > 0)
            log.info("Se vaciaron {} wishlists vencidas", vaciadas);
    }
}
