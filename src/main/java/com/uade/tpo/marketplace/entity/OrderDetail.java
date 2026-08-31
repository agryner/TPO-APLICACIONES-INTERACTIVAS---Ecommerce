package com.uade.tpo.marketplace.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Renglon de una orden de compra.
 *
 * Guarda una copia del nombre, el precio y el descuento que tenia el producto
 * en el momento de la compra. Si despues el vendedor cambia el precio, esta
 * orden sigue mostrando lo que el comprador efectivamente pago.
 *
 * Lo crea OrdenDeCompraServiceImpl al cerrar la compra y se guarda en
 * cascada junto con su OrdenDeCompra.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "order_detail")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_order_detail")
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenDeCompra orden;

    // Referencia al producto, solo para trazabilidad: los datos de abajo son los que valen.
    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer cantidad;

    /** Precio de lista al momento de la compra. */
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /** Descuento vigente al momento de la compra, en porcentaje. */
    @Column(nullable = false)
    private Integer descuento;

    /** precioUnitario con el descuento aplicado, por unidad. */
    public BigDecimal getPrecioFinal() {
        return precioUnitario
                .multiply(BigDecimal.valueOf(100 - descuento))
                .divide(BigDecimal.valueOf(100));
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public BigDecimal getTotal() {
        return getPrecioFinal().multiply(BigDecimal.valueOf(cantidad));
    }
}
