package com.uade.tpo.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.ItemCarrito;

/**
 * Acceso a datos de ItemCarrito.
 *
 * findByCarritoIdAndProductoId le permite a CarritoServiceImpl detectar si el
 * producto ya estaba en el carrito y sumar cantidad en vez de duplicar.
 */
@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    List<ItemCarrito> findByCarritoId(Long idCarrito);

    Optional<ItemCarrito> findByCarritoIdAndProductoId(Long idCarrito, Long idProducto);
}
