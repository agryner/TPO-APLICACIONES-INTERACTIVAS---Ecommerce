package com.uade.tpo.marketplace.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints REST de usuarios.
 *
 * Recibe UsuarioRequest, delega en UsuarioService y devuelve entidades
 * Usuario o MensajeResponse.
 */
@RestController
@RequestMapping("usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<Usuario>> getUsuarios() {
        return ResponseEntity.ok(usuarioService.getUsuarios());
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<Usuario> getUsuarioById(@PathVariable Long idUsuario)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(usuarioService.getUsuarioById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new));
    }

    @PostMapping
    public ResponseEntity<Object> createUsuario(@RequestBody UsuarioRequest request)
            throws UsuarioDuplicadoException {
        Usuario result = usuarioService.createUsuario(request);
        return ResponseEntity.created(URI.create("/usuarios/" + result.getId())).body(result);
    }

    @PutMapping("/{idUsuario}")
    public ResponseEntity<Usuario> updateUsuario(@PathVariable Long idUsuario,
            @RequestBody UsuarioRequest request) throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(usuarioService.updateUsuario(idUsuario, request));
    }

    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<MensajeResponse> deleteUsuario(@PathVariable Long idUsuario)
            throws UsuarioNoEncontradoException {
        usuarioService.deleteUsuario(idUsuario);
        return ResponseEntity.ok(new MensajeResponse("Usuario eliminado correctamente"));
    }
}
