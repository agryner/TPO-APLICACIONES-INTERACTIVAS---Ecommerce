package com.uade.tpo.marketplace.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.OrdenDeCompra;
import com.uade.tpo.marketplace.entity.dto.OrdenDeCompraRequest;
import com.uade.tpo.marketplace.exceptions.CarritoVacioException;
import com.uade.tpo.marketplace.exceptions.OrdenNoEncontradaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.StockInsuficienteException;
import com.uade.tpo.marketplace.service.OrdenDeCompraService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("ordenes")
@RequiredArgsConstructor
public class OrdenesController {

    private final OrdenDeCompraService ordenService;

    @GetMapping
    public ResponseEntity<List<OrdenDeCompra>> getOrdenes(
            @RequestParam(required = false) Long idUsuario) {
        if (idUsuario != null)
            return ResponseEntity.ok(ordenService.getOrdenesByUsuario(idUsuario));

        return ResponseEntity.ok(ordenService.getOrdenes());
    }

    @GetMapping("/{idOrden}")
    public ResponseEntity<OrdenDeCompra> getOrdenById(@PathVariable Long idOrden)
            throws OrdenNoEncontradaException {
        return ResponseEntity.ok(ordenService.getOrdenById(idOrden)
                .orElseThrow(OrdenNoEncontradaException::new));
    }

    @PostMapping
    public ResponseEntity<Object> createOrden(@RequestBody OrdenDeCompraRequest request)
            throws UsuarioNoEncontradoException, CarritoVacioException, StockInsuficienteException {
        OrdenDeCompra result = ordenService.createOrden(request);
        return ResponseEntity.created(URI.create("/ordenes/" + result.getId())).body(result);
    }

    @PutMapping("/{idOrden}/estado")
    public ResponseEntity<OrdenDeCompra> actualizarEstado(@PathVariable Long idOrden,
            @RequestParam String estado) throws OrdenNoEncontradaException {
        return ResponseEntity.ok(ordenService.actualizarEstado(idOrden, estado));
    }
}
