package com.uade.tpo.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.Usuario;

/**
 * Acceso a datos de Usuario.
 *
 * Los finder por mail y por nombreUsuario son los que usa UsuarioServiceImpl
 * para detectar duplicados antes de dar de alta.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByMail(String mail);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}
