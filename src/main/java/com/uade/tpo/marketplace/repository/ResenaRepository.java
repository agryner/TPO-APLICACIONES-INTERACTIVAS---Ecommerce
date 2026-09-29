package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Resena;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByProductoIdOrderByFechaDesc(Long idProducto);

    List<Resena> findByProductoVendedorId(Long idVendedor);

    boolean existsByOrdenIdAndProductoId(Long idOrden, Long idProducto);

    /**
     * El promedio y la cantidad de cada vendedor, de una sola consulta. Pedirlo
     * vendedor por vendedor seria una consulta por producto del catalogo.
     */
    @Query("SELECT r.producto.vendedor.id, AVG(r.puntaje), COUNT(r) "
            + "FROM Resena r GROUP BY r.producto.vendedor.id")
    List<Object[]> resumenPorVendedor();

    /**
     * Lo mismo pero por producto, para la calificacion que va en cada tarjeta
     * del catalogo.
     */
    @Query("SELECT r.producto.id, AVG(r.puntaje), COUNT(r) "
            + "FROM Resena r GROUP BY r.producto.id")
    List<Object[]> resumenPorProducto();
}
