package com.uade.tpo.marketplace.entity;

import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private Integer vendidos = 0;

    @Column(nullable = false)
    private Integer vistos = 0;

    @Column(name = "admite_envio", nullable = false)
    private Boolean admiteEnvio = true;

    @Column(name = "acepta_ofertas", nullable = false)
    private Boolean aceptaOfertas = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "provincia", nullable = false)
    private Provincia provincia;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion", nullable = false)
    private CondicionProducto condicion;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_destacado", nullable = false)
    private NivelDestacado nivelDestacado = NivelDestacado.NINGUNO;

    @Column(name = "destacado_hasta")
    private LocalDateTime destacadoHasta;

    /**
     * Pre : nada.
     * Post: el nivel que vale HOY. Un destacado vencido no ordena nada aunque
     *       la columna todavia diga PREMIUM: la tarea que limpia corre cada
     *       tanto, y entre medio nadie tiene que ver un destacado que ya se
     *       pago y se termino.
     */
    public NivelDestacado nivelVigente() {
        if (nivelDestacado == null || destacadoHasta == null
                || destacadoHasta.isBefore(LocalDateTime.now()))
            return NivelDestacado.NINGUNO;

        return nivelDestacado;
    }

    @Column
    private String descripcion;

    @Column
    private String ubicacion;

    @Column
    private Integer descuento;

    @Column(nullable = false)
    private Boolean activo = true;

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
