package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.OrdenDeCompra;

/**
 * Acceso a datos de OrdenDeCompra.
 *
 * Lo usa OrdenDeCompraServiceImpl para listar el historial de un usuario.
 */
@Repository
public interface OrdenDeCompraRepository extends JpaRepository<OrdenDeCompra, Long> {

    List<OrdenDeCompra> findByUsuarioId(Long idUsuario);
}
