package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.UsuarioRequest;
import com.uade.tpo.marketplace.entity.dto.UsuarioResponse;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.entity.Producto;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.repository.UsuarioRepository;
import com.uade.tpo.marketplace.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;

/**
 * Logica de usuarios: altas, ediciones y control de duplicados.
 *
 * Lo llama UsuariosController y se apoya en UsuarioRepository para verificar
 * que el mail y el nombre de usuario no esten tomados.
 */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacion;
    private final ProductoRepository productoRepository;
    private final CarritoService carritoService;

    public List<UsuarioResponse> getUsuarios() {
        return usuarioRepository.findAll().stream()
                .filter(Usuario::getActivo)
                .map(UsuarioResponse::from)
                .toList();
    }

    public UsuarioResponse getUsuarioById(Long idUsuario) throws UsuarioNoEncontradoException {
        return usuarioRepository.findById(idUsuario)
                .map(UsuarioResponse::from)
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    public UsuarioResponse createUsuario(UsuarioRequest request) throws UsuarioDuplicadoException {
        if (usuarioRepository.findByMail(request.getMail()).isPresent()
                || usuarioRepository.findByNombreUsuario(request.getNombreUsuario()).isPresent())
            throw new UsuarioDuplicadoException();

        Usuario usuario = new Usuario();
        copiarDatos(usuario, request);

        // El rol NO sale del body. Mandando "rol": "ADMIN" en el alta publica
        // cualquiera se hacia administrador, y por la edicion cualquiera se
        // ascendia a si mismo. Un ADMIN se crea a mano en la base.
        usuario.setRol(TipoUsuario.CLIENTE);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    public UsuarioResponse updateUsuario(Long idUsuario, UsuarioRequest request, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        copiarDatos(usuario, request);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Baja logica: el usuario se marca inactivo en vez de borrarse.
     *
     * Sus ordenes son el registro de operaciones que ocurrieron y siguen
     * apuntando a el, asi que un DELETE real las arrastraria, incluidas las
     * ventas de los vendedores que le vendieron.
     */
    public void deleteUsuario(Long idUsuario, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException, CuentaInactivaException {
        // Cada uno da de baja su propia cuenta; el ADMIN, la de cualquiera.
        autorizacion.validarDuenio(idSolicitante, idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        // Sin esto la baja quedaba a medias: el vendedor desaparecia de los
        // listados pero sus publicaciones seguian en el catalogo y se compraban
        // igual. Se dan de baja tambien, y salen de los carritos ajenos.
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

    private void copiarDatos(Usuario usuario, UsuarioRequest request) {
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setNombreUsuario(request.getNombreUsuario());
        usuario.setMail(request.getMail());
        usuario.setContrasena(request.getContrasena());
        usuario.setDireccion(request.getDireccion());
    }
}
