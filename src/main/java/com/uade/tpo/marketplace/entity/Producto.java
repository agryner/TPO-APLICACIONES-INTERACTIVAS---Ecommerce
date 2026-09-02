package com.uade.tpo.marketplace.entity;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Producto publicado por un vendedor.
 *
 * Cuelga de una Categoria y de un Usuario vendedor, y agrupa sus Foto en
 * cascade ALL, asi que borrar el producto borra sus fotos. Lo persiste
 * ProductoRepository y el controller lo devuelve tal cual en el JSON.
 */
@Data
@NoArgsConstructor
@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column
    private String descripcion;

    @Column
    private String ubicacion;

    // Porcentaje de descuento, 0 a 100.
    @Column
    private Integer descuento;

    /**
     * Baja logica. Un registro inactivo desaparece de los listados pero sigue
     * en la base, porque las ordenes ya cerradas lo referencian y borrarlo de
     * verdad se llevaria puesto ese historial.
     */
    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * Un producto nace en BORRADOR y se publica al subirle la primera foto.
     * Es distinto de activo, que marca la baja logica.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_publicacion", nullable = false)
    private EstadoPublicacion estadoPublicacion = EstadoPublicacion.BORRADOR;

    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Foto> fotos;
}
