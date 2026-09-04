package com.uade.tpo.marketplace.entity;

import java.time.LocalDateTime;

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
 * Un producto guardado en la wishlist de alguien.
 *
 * No lleva cantidad: querer algo dos veces no significa nada. Por eso agregar
 * un producto que ya estaba no hace nada en vez de acumular, que es lo que si
 * hace el carrito.
 *
 * Guarda cuando se agrego para poder ordenar la lista por lo mas reciente.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "item_wishlist")
public class ItemWishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "id_wishlist", nullable = false)
    private Wishlist wishlist;

    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "fecha_agregado", nullable = false)
    private LocalDateTime fechaAgregado;
}
