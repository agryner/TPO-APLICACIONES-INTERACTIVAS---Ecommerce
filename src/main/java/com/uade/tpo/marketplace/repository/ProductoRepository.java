package com.uade.tpo.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Producto;

/**
 * Acceso a datos de Producto.
 *
 * El filtrado del catalogo lo arma ProductoServiceImpl con streams sobre
 * findAll(); lo unico propio de aca es el chequeo de existencia por categoria.
 * Lo usan ese service y los que necesitan resolver un producto por id.
 *
 * El bloqueo de filas del checkout no vive aca: OrdenDeCompraServiceImpl lo
 * hace con un refresh del EntityManager, porque necesita releer la fila y no
 * solo traerla.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Lo usa CategoriaServiceImpl antes de borrar: pregunta si queda algun
     * producto colgado de esa categoria sin traerselos.
     */
    boolean existsByCategoriaId(Long idCategoria);
}
