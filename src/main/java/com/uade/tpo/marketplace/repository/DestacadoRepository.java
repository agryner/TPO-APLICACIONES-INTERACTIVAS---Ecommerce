package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Destacado;

@Repository
public interface DestacadoRepository extends JpaRepository<Destacado, Long> {
    List<Destacado> findByProductoVendedorIdOrderByDesdeDesc(Long idVendedor);

    List<Destacado> findByOrderByDesdeDesc();
}
