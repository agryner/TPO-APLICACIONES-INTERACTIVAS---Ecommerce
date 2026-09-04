package com.uade.tpo.marketplace.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Lista de deseos de un usuario: lo que quiere comprar mas adelante.
 *
 * Se parece al Carrito -uno por usuario, con items y vencimiento- pero no es lo
 * mismo y por eso es una entidad aparte. El carrito es una compra a punto de
 * cerrarse: sus items tienen cantidad, suman totales y desaparecen cuando el
 * producto sale de circulacion. Esto es una lista de intenciones: sin cantidad,
 * sin totales, y los productos se quedan aunque se pausen, porque el punto es
 * justamente poder volver a mirarlos cuando vuelvan.
 *
 * De ahi tambien la diferencia de plazo: el carrito vence en un mes, esto en
 * ocho.
 */
@Data
@NoArgsConstructor
@Entity
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_wishlist")
    private Long id;

    // Un usuario tiene exactamente una wishlist.
    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    /**
     * Cuando se vacia sola si nadie la toca. Null mientras esta vacia: una lista
     * sin nada adentro no tiene por que vencer.
     */
    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemWishlist> items = new ArrayList<>();
}
