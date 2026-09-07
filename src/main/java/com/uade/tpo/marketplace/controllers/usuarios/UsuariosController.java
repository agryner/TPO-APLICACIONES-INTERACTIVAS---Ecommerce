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

    /**
     * Los usuarios en actividad.
     *
     * Pre : solo el token.
     * Post: la lista sin los dados de baja y sin el campo contrasena, que el
     *       DTO ni siquiera tiene.
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getUsuarios() {
        return ResponseEntity.ok(usuarioService.getUsuarios());
    }

    /**
     * Quien soy: los datos de la cuenta del token.
     *
     * No recibe ningun id. Es el complemento del login, que devuelve solo el
     * token: con esto el frontend sabe a quien saludar y si mostrar el panel de
     * administracion, sin tener que decodificar el JWT por su cuenta.
     *
     * Va declarado antes que /{idUsuario} porque si no Spring tomaria "me"
     * como un id y fallaria al convertirlo a Long.
     *
     * Pre : solo el token.
     * Post: los datos publicos de esa cuenta. Se leen de la base en cada
     *       pedido, asi que reflejan un cambio de nombre o de rol sin esperar
     *       a que el usuario se vuelva a loguear.
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> yo(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    /**
     * Un usuario puntual.
     *
     * Pre : el id en la ruta y el token.
     * Post: sus datos publicos. 404 si no existe.
     */
    @GetMapping("/{idUsuario}")
    public ResponseEntity<UsuarioResponse> getUsuarioById(@PathVariable Long idUsuario)
            throws UsuarioNoEncontradoException {
        return ResponseEntity.ok(usuarioService.getUsuarioById(idUsuario));
    }

    /**
     * Cambia los datos de mi cuenta.
     *
     * No recibe ningun id: se edita siempre la del token. El ADMIN no entra
     * aca, porque moderar no incluye reescribir los datos personales de otro;
     * para lo suyo tiene reactivar, cambiar el rol y dar de baja.
     *
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

    /** Solo ADMIN: devuelve al ruedo una cuenta dada de baja. */
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
     * Solo ADMIN. El rol llega como enum, asi que Spring rechaza con 400
     * cualquier valor que no sea ADMIN o CLIENTE.
     *
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
     * Da de baja la cuenta de otro. Para la propia esta /usuarios/me.
     *
     * Pre : el id en la ruta y un token de ADMIN.
     * Post: un mensaje de confirmacion. Es baja logica y arrastra las
     *       publicaciones del usuario, que salen del catalogo y de los
     *       carritos ajenos. Las ordenes se conservan.
     */
    /**
     * Darme de baja a mi mismo.
     *
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

    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<MensajeResponse> deleteUsuario(@PathVariable Long idUsuario,
            @AuthenticationPrincipal Usuario usuario)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        usuarioService.deleteUsuario(idUsuario, usuario.getId());
        return ResponseEntity.ok(new MensajeResponse("Usuario dado de baja correctamente"));
    }
}
