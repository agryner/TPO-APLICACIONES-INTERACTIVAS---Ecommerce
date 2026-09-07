package com.uade.tpo.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Orden de compra: lo que un comprador le compro a un vendedor puntual.
 *
 * Agrupa sus OrderDetail en cascade ALL y congela subtotal y total al momento
 * de la compra. La persiste OrdenDeCompraRepository.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "orden_de_compra")
public class OrdenDeCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_comprador", nullable = false)
    private Usuario comprador;

    /**
     * Toda orden es una transaccion entre dos personas, asi que tiene un unico
     * vendedor. Si el carrito mezcla productos de varios, OrdenDeCompraServiceImpl
     * genera una orden por cada uno en vez de guardar aca un dato incompleto.
     */
    @ManyToOne
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderDetail> items = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    /**
     * Cuando se cerro la compra. No cambia nunca.
     *
     * Es el dato del que cuelga casi todo lo que se puede medir despues:
     * cuantas ventas tuvo alguien en un mes, si un producto se vende rapido,
     * como evoluciono el volumen. Si no se guarda en el momento, no hay forma
     * de reconstruirlo.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Cuando fue el ultimo cambio de estado.
     *
     * Con esta y la de creacion alcanza para lo que importa: cuanto tarda un
     * vendedor en despachar, o cuanto lleva una orden trabada esperando a
     * alguien. Lo que no se puede es reconstruir el camino completo -cuando
     * paso por cada estado por separado-; para eso haria falta una tabla de
     * historial, que por ahora no justifica el costo.
     */
    @Column(name = "fecha_ultimo_estado", nullable = false)
    private LocalDateTime fechaUltimoEstado;
}
