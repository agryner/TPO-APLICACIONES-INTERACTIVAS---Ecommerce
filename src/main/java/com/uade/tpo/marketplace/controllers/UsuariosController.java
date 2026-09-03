package com.uade.tpo.marketplace.controllers;

import com.uade.tpo.marketplace.entity.dto.MensajeResponse;
import com.uade.tpo.marketplace.entity.dto.UsuarioRequest;
import com.uade.tpo.marketplace.entity.dto.UsuarioResponse;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CambioDeRolInvalidoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.service.UsuarioService;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

/**
 * Endpoints REST de usuarios.
 *
 * Recibe UsuarioRequest, delega en UsuarioService y devuelve
 * UsuarioResponse o MensajeResponse.
 */
@RestController
@RequestMapping("usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getUsuarios() {
        return ResponseEntity.ok(usuarioService.getUsuarios());
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<UsuarioResponse> getUsuarioById(@PathVariable Long idUsuario)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(usuarioService.getUsuarioById(idUsuario));
    }

    @PostMapping
    public ResponseEntity<Object> createUsuario(@Valid @RequestBody UsuarioRequest request)
            throws UsuarioDuplicadoException {
        UsuarioResponse result = usuarioService.createUsuario(request);
        return ResponseEntity.created(URI.create("/usuarios/" + result.getId())).body(result);
    }

    @PutMapping("/{idUsuario}")
    public ResponseEntity<UsuarioResponse> updateUsuario(@PathVariable Long idUsuario,
            @Valid @RequestBody UsuarioRequest request, @RequestParam Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        return ResponseEntity.ok(usuarioService.updateUsuario(idUsuario, request, idSolicitante));
    }

    /** Solo ADMIN: devuelve al ruedo una cuenta dada de baja. */
    @PutMapping("/{idUsuario}/reactivar")
    public ResponseEntity<UsuarioResponse> reactivar(@PathVariable Long idUsuario,
            @RequestParam Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException {
        return ResponseEntity.ok(usuarioService.reactivarUsuario(idUsuario, idSolicitante));
    }

    /**
     * Solo ADMIN. El rol llega como enum, asi que Spring rechaza con 400
     * cualquier valor que no sea ADMIN o CLIENTE.
     */
    @PutMapping("/{idUsuario}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long idUsuario,
            @RequestParam TipoUsuario rol, @RequestParam Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException,
            CambioDeRolInvalidoException {
        return ResponseEntity.ok(usuarioService.cambiarRol(idUsuario, rol, idSolicitante));
    }

    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<MensajeResponse> deleteUsuario(@PathVariable Long idUsuario,
            @RequestParam Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        usuarioService.deleteUsuario(idUsuario, idSolicitante);
        return ResponseEntity.ok(new MensajeResponse("Usuario dado de baja correctamente"));
    }
}
