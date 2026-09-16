package com.uade.tpo.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.marketplace.entity.ItemWishlist;

@Repository
public interface ItemWishlistRepository extends JpaRepository<ItemWishlist, Long> {
    List<ItemWishlist> findByProductoId(Long idProducto);
}
