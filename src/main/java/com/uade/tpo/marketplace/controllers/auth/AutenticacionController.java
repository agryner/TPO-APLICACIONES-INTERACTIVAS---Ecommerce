package com.uade.tpo.marketplace.controllers.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.controllers.auth.LoginRequest;
import com.uade.tpo.marketplace.controllers.auth.TokenResponse;
import com.uade.tpo.marketplace.controllers.usuarios.UsuarioRequest;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.AutenticacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AutenticacionController {
    private final AutenticacionService autenticacionService;

    /**
     * Pre : el body con nombre, apellido, nombreUsuario, mail, contrasena y
     *       direccion. El campo rol se acepta pero se ignora: todos nacen
     *       CLIENTE.
     * Post: 201 con el token en access_token. 400 si el mail o el nombre de
     *       usuario ya estan tomados.
     */
    @PostMapping("/registro")
    public ResponseEntity<TokenResponse> registrar(@Valid @RequestBody UsuarioRequest request)
            throws UsuarioDuplicadoException, UsuarioNoEncontradoException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(autenticacionService.registrar(request));
    }

    /**
     * Pre : el body con mail y contrasena.
     * Post: 200 con el token en access_token. 401 si el mail no existe, si la
     *       contrasena no coincide o si la cuenta esta dada de baja, con el
     *       mismo mensaje en los tres casos.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(autenticacionService.login(request));
    }
}
