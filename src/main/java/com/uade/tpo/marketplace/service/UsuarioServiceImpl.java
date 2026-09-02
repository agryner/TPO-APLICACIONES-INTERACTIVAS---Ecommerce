package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.entity.dto.UsuarioRequest;
import com.uade.tpo.marketplace.entity.dto.UsuarioResponse;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

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
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    public UsuarioResponse updateUsuario(Long idUsuario, UsuarioRequest request, Long idSolicitante)
            throws UsuarioNoEncontradoException, OperacionAjenaException {
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
            throws UsuarioNoEncontradoException, OperacionAjenaException {
        autorizacion.validarDuenio(idSolicitante, idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private void copiarDatos(Usuario usuario, UsuarioRequest request) {
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setNombreUsuario(request.getNombreUsuario());
        usuario.setMail(request.getMail());
        usuario.setContrasena(request.getContrasena());
        usuario.setDireccion(request.getDireccion());
        usuario.setRol(request.getRol() == null ? TipoUsuario.CLIENTE : request.getRol());
    }
}
