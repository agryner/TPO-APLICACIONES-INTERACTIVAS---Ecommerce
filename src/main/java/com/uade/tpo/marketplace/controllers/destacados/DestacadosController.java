package com.uade.tpo.marketplace.controllers.destacados;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.SinResultadosException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.DestacadoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("destacados")
@RequiredArgsConstructor
public class DestacadosController {
    private final DestacadoService destacadoService;

    /**
     * Pre : solo el token.
     * Post: el historial de visibilidad pagada. El ADMIN ve el de todos, para
     *       saber que se vendio; un vendedor ve solo el de sus productos. Cada
     *       fila dice nivel, meses, desde y hasta, y si sigue vigente. 404 si
     *       no hay ninguno.
     */
    @GetMapping
    public ResponseEntity<List<DestacadoResponse>> getHistorial(
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, SinResultadosException {
        return ResponseEntity.ok(destacadoService.getHistorial(usuario.getId()));
    }
}
