package com.uade.tpo.marketplace.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Producto;

/**
 * Acceso a datos de Producto.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCategoriaId(Long idCategoria);

    List<Producto> findByVendedorId(Long idVendedor);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    List<Producto> findByPrecioBetween(BigDecimal min, BigDecimal max);
}
