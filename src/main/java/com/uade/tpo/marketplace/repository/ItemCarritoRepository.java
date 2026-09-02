package com.uade.tpo.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.ItemCarrito;

/**
 * Acceso a datos de ItemCarrito.
 *
 * Para operar sobre un carrito puntual CarritoServiceImpl no lo necesita: llega
 * a los items por la coleccion del Carrito, que los persiste en cascada. Lo usa
 * para lo que si requiere mirar de costado: encontrar un producto en carritos
 * ajenos cuando deja de estar disponible.
 */
@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    List<ItemCarrito> findByCarritoId(Long idCarrito);

    /**
     * Todos los items que tienen ese producto, de cualquier carrito.
     *
     * Lo usa CarritoServiceImpl para sacar de circulacion un producto que dejo
     * de estar disponible: sin esto habria que recorrer todos los carritos.
     */
    List<ItemCarrito> findByProductoId(Long idProducto);

    Optional<ItemCarrito> findByCarritoIdAndProductoId(Long idCarrito, Long idProducto);
}
