package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Producto;

/**
 * Acceso a datos de Producto.
 *
 * Los tres finder cubren los filtros que expone el controller (categoria,
 * vendedor y nombre). Lo usan ProductoServiceImpl y los services que
 * necesitan resolver un producto por id.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCategoriaId(Long idCategoria);

    List<Producto> findByVendedorId(Long idVendedor);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);
}
