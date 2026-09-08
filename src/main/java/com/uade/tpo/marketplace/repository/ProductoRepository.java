package com.uade.tpo.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByCategoriaId(Long idCategoria);
}
