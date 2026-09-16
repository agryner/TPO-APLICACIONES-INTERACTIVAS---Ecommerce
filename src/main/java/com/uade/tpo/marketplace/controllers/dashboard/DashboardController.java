package com.uade.tpo.marketplace.controllers.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    /**
     * Pre : solo el token. No recibe ningun id: el tablero es siempre el
     *       propio.
     * Post: las cuatro secciones, en el orden en que el vendedor las mira: lo
     *       que requiere accion, la plata, el rendimiento de las publicaciones
     *       y la reputacion. 403 para el ADMIN y el DESPACHANTE, que no venden.
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDelVendedor(
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, RolNoComerciaException {
        return ResponseEntity.ok(dashboardService.getDelVendedor(usuario.getId()));
    }
}
