package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Resena;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByProductoIdOrderByFechaDesc(Long idProducto);

    List<Resena> findByProductoVendedorId(Long idVendedor);

    boolean existsByOrdenIdAndProductoId(Long idOrden, Long idProducto);
}
