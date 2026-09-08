package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.usuarios.UsuarioRequest;
import com.uade.tpo.marketplace.controllers.usuarios.UsuarioResponse;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CambioDeRolInvalidoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.repository.UsuarioRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;
    private final PasswordEncoder passwordEncoder;
    private final ProductoRepository productoRepository;
    private final CarritoService carritoService;

    /**
     * Pre : el id de quien pide, que tiene que ser ADMIN.
     * Post: los usuarios activos, sin los dados de baja. 403 si no es ADMIN.
     */
    public List<UsuarioResponse> getUsuarios(Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        return usuarioRepository.findAll().stream()
                .filter(Usuario::getActivo)
                .map(UsuarioResponse::from)
                .toList();
    }

    /**
     * Pre : el id buscado y el de quien pide, que tiene que ser ADMIN.
     * Post: el usuario, este activo o no. 403 si no es ADMIN, 404 si no existe.
     */
    public UsuarioResponse getUsuarioById(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException {
        autorizacion.validarAdmin(idSolicitante);

        return usuarioRepository.findById(idUsuario)
                .map(UsuarioResponse::from)
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    /**
     * Pre : el request con los datos de la cuenta.
     * Post: el usuario creado, con la contrasena hasheada y el rol forzado a
     *       CLIENTE. Tira UsuarioDuplicadoException si el mail o el nombre de
     *       usuario ya estan tomados.
     */
    public UsuarioResponse createUsuario(UsuarioRequest request) throws UsuarioDuplicadoException {
        if (usuarioRepository.findByMail(request.getMail()).isPresent()
                || usuarioRepository.findByNombreUsuario(request.getNombreUsuario()).isPresent())
            throw new UsuarioDuplicadoException();

        Usuario usuario = new Usuario();
        copiarDatos(usuario, request);

        usuario.setRol(TipoUsuario.CLIENTE);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Pre : el id, el request y el id de quien pide.
     * Post: el usuario actualizado, con la contrasena vuelta a hashear. El rol
     *       no se toca desde aca.
     */
    public UsuarioResponse updateUsuario(Long idUsuario, UsuarioRequest request)
            throws UsuarioNoEncontradoException, CuentaInactivaException {
        autorizacion.validarActivo(idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        copiarDatos(usuario, request);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Pre : el id y el id de quien pide, que tiene que ser ADMIN.
     * Post: el usuario activo otra vez. No reactiva sus publicaciones a
     *       proposito: entre ellas pueden estar las que el mismo habia dado de
     *       baja.
     */
    @Transactional
    public UsuarioResponse reactivarUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException {
        autorizacion.validarAdmin(idSolicitante);
        autorizacion.validarActivo(idSolicitante);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        usuario.setActivo(true);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Pre : el id, el rol destino y el id de quien pide, que tiene que ser
     *       ADMIN.
     * Post: el usuario con el rol nuevo. Tira CambioDeRolInvalidoException si
     *       un admin intenta degradarse a si mismo: si el ultimo se saca el
     *       rol, no queda quien promueva a nadie.
     */
    @Transactional
    public UsuarioResponse cambiarRol(Long idUsuario, TipoUsuario rol, Long idSolicitante)
            throws UsuarioNoEncontradoException, AccesoDenegadoException, CuentaInactivaException,
            CambioDeRolInvalidoException {
        autorizacion.validarAdmin(idSolicitante);
        autorizacion.validarActivo(idSolicitante);

        if (idUsuario.equals(idSolicitante) && rol != TipoUsuario.ADMIN)
            throw new CambioDeRolInvalidoException();

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        usuario.setRol(rol);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Pre : el id y el id de quien pide.
     * Post: nada. Baja logica que arrastra las publicaciones del usuario:
     *       salen del catalogo y de los carritos ajenos. Las ordenes se
     *       conservan.
     */
    public void deleteUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        for (Producto producto : productoRepository.findAll()) {
            if (producto.getVendedor() != null
                    && producto.getVendedor().getId().equals(idUsuario)
                    && Boolean.TRUE.equals(producto.getActivo())) {
                producto.setActivo(false);
                productoRepository.save(producto);
                carritoService.quitarDeTodosLosCarritos(producto.getId());
            }
        }
    }

    /**
     * Pre : el usuario y el request.
     * Post: nada. Vuelca los campos sobre la entidad hasheando la contrasena.
     *       No copia el rol.
     */
    private void copiarDatos(Usuario usuario, UsuarioRequest request) {
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setNombreUsuario(request.getNombreUsuario());
        usuario.setMail(request.getMail());
        usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setDireccion(request.getDireccion());
    }
}
