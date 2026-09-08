package com.uade.tpo.marketplace.controllers.usuarios;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.marketplace.controllers.common.MensajeResponse;
import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CambioDeRolInvalidoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("usuarios")
@RequiredArgsConstructor
public class UsuariosController {
    private final UsuarioService usuarioService;

    /**
     * Pre : el token de un ADMIN.
     * Post: la lista sin los dados de baja y sin el campo contrasena, que el
     *       DTO ni siquiera tiene. 403 si quien pide no es ADMIN.
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getUsuarios(
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(usuarioService.getUsuarios(usuario.getId()));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> yo(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    /**
     * Pre : el id en la ruta y el token de un ADMIN.
     * Post: los datos de esa cuenta. 403 si quien pide no es ADMIN, 404 si el
     *       id no existe.
     */
    @GetMapping("/{idUsuario}")
    public ResponseEntity<UsuarioResponse> getUsuarioById(@PathVariable Long idUsuario,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        return ResponseEntity.ok(usuarioService.getUsuarioById(idUsuario, usuario.getId()));
    }

    /**
     * Pre : el body completo y el token.
     * Post: el usuario actualizado. La contrasena se vuelve a hashear. El rol
     *       no se toca desde aca: para eso esta el endpoint de rol.
     */
    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> updateUsuario(
            @Valid @RequestBody UsuarioRequest request, @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        return ResponseEntity.ok(usuarioService.updateUsuario(usuario.getId(), request));
    }

    /**
     * Pre : el id en la ruta y un token de ADMIN.
     * Post: el usuario activo otra vez. No reactiva sus publicaciones: cada
     *       producto se reactiva por separado.
     */
    @PutMapping("/{idUsuario}/reactivar")
    public ResponseEntity<UsuarioResponse> reactivar(@PathVariable Long idUsuario,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException {
        return ResponseEntity.ok(usuarioService.reactivarUsuario(idUsuario, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta, el rol destino como enum, y un token de ADMIN.
     * Post: el usuario con el rol nuevo. 409 si un admin intenta quitarse el
     *       rol a si mismo, 400 si el rol no existe.
     */
    @PutMapping("/{idUsuario}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long idUsuario,
            @RequestParam TipoUsuario rol, @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException,
            CambioDeRolInvalidoException {
        return ResponseEntity.ok(usuarioService.cambiarRol(idUsuario, rol, usuario.getId()));
    }

    /**
     * Pre : el id en la ruta y un token de ADMIN.
     * Post: un mensaje de confirmacion. Es baja logica y arrastra las
     *       publicaciones del usuario, que salen del catalogo y de los
     *       carritos ajenos. Las ordenes se conservan.
     * Pre : solo el token.
     * Post: un mensaje de confirmacion. Es baja logica y arrastra las
     *       publicaciones propias: salen del catalogo y de los carritos
     *       ajenos. Las ordenes se conservan.
     */
    @DeleteMapping("/me")
    public ResponseEntity<MensajeResponse> bajaPropia(@AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        usuarioService.deleteUsuario(usuario.getId(), usuario.getId());
        return ResponseEntity.ok(new MensajeResponse("Usuario dado de baja correctamente"));
    }

    /**
     * Pre : el id en la ruta y el token de un ADMIN.
     * Post: un mensaje de confirmacion. Es baja logica y arrastra las
     *       publicaciones de esa cuenta: salen del catalogo y de los carritos
     *       ajenos. Las ordenes se conservan.
     */
    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<MensajeResponse> deleteUsuario(@PathVariable Long idUsuario,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        usuarioService.deleteUsuario(idUsuario, usuario.getId());
        return ResponseEntity.ok(new MensajeResponse("Usuario dado de baja correctamente"));
    }
}
