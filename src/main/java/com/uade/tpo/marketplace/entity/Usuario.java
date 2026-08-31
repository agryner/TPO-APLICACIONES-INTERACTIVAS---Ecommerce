    package com.uade.tpo.marketplace.entity;

    import java.util.List;

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
     */
    @Data
    @NoArgsConstructor
    @Entity
    public class Usuario {

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

        @JsonIgnore
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        @OneToMany(mappedBy = "usuario")
        private List<OrdenDeCompra> ordenes;
    }
