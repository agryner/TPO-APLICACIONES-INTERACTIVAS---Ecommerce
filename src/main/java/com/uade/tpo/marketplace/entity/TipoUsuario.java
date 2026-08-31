package com.uade.tpo.marketplace.entity;

/**
 * Roles posibles de un Usuario.
 *
 * Se guarda como texto en la columna rol gracias al @Enumerated(STRING) de
 * la entidad, asi que agregar valores no rompe los datos existentes.
 */
public enum TipoUsuario {
    ADMIN,
    CLIENTE
}
