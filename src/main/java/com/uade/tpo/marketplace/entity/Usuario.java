package com.uade.tpo.marketplace.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Usuario del marketplace: compra, vende, o las dos cosas.
 *
 * Es la punta de la que cuelgan sus productos, sus carritos y sus ordenes.
 * Lo persiste UsuarioRepository.
 *
 * Implementa UserDetails para que Spring Security pueda autenticarlo sin una
 * clase puente en el medio, que es como lo hace el ejemplo de la catedra. La
 * entidad queda sabiendo de seguridad, que no es ideal, pero evita mantener dos
 * objetos en espejo.
 *
 * Los metodos del final traducen los nombres del dominio a los que espera
 * Spring: el "username" que identifica al usuario es su mail, y la "password"
 * es contrasena. Los que no estan declarados -isAccountNonExpired y los otros
 * dos- vienen por defecto en true desde la interfaz.
 */
@Data
@NoArgsConstructor
@Entity
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(name = "nombre_usuario", nullable = false, unique = true)
    private String nombreUsuario;

    @Column(nullable = false, unique = true)
    private String mail;

    // WRITE_ONLY: se puede recibir en el JSON de alta, pero nunca se devuelve.
    @JsonProperty(access = Access.WRITE_ONLY)
    @Column(nullable = false)
    private String contrasena;

    @Column
    private String direccion;

    /**
     * Baja logica. Un registro inactivo desaparece de los listados pero sigue
     * en la base, porque las ordenes ya cerradas lo referencian y borrarlo de
     * verdad se llevaria puesto ese historial.
     */
    @Column(nullable = false)
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private TipoUsuario rol;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "vendedor")
    private List<Producto> productos;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "usuario")
    private List<Carrito> carritos;

    /** Ordenes en las que este usuario es el que compro. */
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "comprador")
    private List<OrdenDeCompra> compras;

    /** Ordenes en las que este usuario es el que vendio. */
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "vendedor")
    private List<OrdenDeCompra> ventas;

    // ---------------------------------------------------------------- UserDetails

    /**
     * El rol, en el formato que Spring entiende.
     *
     * Va sin el prefijo ROLE_, asi que las reglas se escriben con
     * hasAuthority("ADMIN") y no con hasRole("ADMIN"), que buscaria ROLE_ADMIN y
     * no encontraria nada.
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.name()));
    }

    /** Spring llama password a lo que aca se llama contrasena. */
    @Override
    @JsonIgnore
    public String getPassword() {
        return contrasena;
    }

    /** El mail es lo que identifica al usuario al loguearse y viaja en el token. */
    @Override
    @JsonIgnore
    public String getUsername() {
        return mail;
    }

    /**
     * Una cuenta dada de baja no puede autenticarse.
     *
     * Con esto el rechazo ocurre en el filtro, antes de llegar a ningun
     * controller, y deja de depender de que cada service se acuerde de llamar a
     * validarActivo.
     */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return Boolean.TRUE.equals(activo);
    }
}
