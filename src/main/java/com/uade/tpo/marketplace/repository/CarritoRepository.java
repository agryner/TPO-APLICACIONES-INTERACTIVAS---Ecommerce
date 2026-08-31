package com.uade.tpo.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Carrito;

/**
 * Acceso a datos de Carrito.
 *
 * findByUsuarioId es la entrada natural, porque el carrito se identifica por
 * su duenio y no por id propio. Lo usa CarritoServiceImpl.
 */
@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioId(Long idUsuario);
}
