package com.uade.tpo.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Wishlist;

/**
 * Acceso a datos de Wishlist.
 *
 * Los items no tienen repositorio propio: se manejan por la coleccion de la
 * Wishlist, que los persiste en cascada. A diferencia del carrito, nadie
 * necesita buscar un producto en las wishlists ajenas, porque cuando un
 * producto sale de circulacion la wishlist lo conserva.
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /** Hay una sola por usuario. */
    Optional<Wishlist> findByUsuarioId(Long idUsuario);
}
