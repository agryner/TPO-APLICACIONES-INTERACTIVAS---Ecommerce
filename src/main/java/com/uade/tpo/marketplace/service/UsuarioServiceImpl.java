package com.uade.tpo.marketplace.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.uade.tpo.marketplace.entity.TipoUsuario;
import com.uade.tpo.marketplace.entity.Usuario;
import com.uade.tpo.marketplace.entity.dto.UsuarioRequest;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;
import com.uade.tpo.marketplace.exceptions.UsuarioDuplicadoException;
import com.uade.tpo.marketplace.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public List<Usuario> getUsuarios() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> getUsuarioById(Long idUsuario) {
        return usuarioRepository.findById(idUsuario);
    }

    public Usuario createUsuario(UsuarioRequest request) throws UsuarioDuplicadoException {
        if (usuarioRepository.findByMail(request.getMail()).isPresent()
                || usuarioRepository.findByNombreUsuario(request.getNombreUsuario()).isPresent())
            throw new UsuarioDuplicadoException();

        Usuario usuario = new Usuario();
        copiarDatos(usuario, request);
        return usuarioRepository.save(usuario);
    }

    public Usuario updateUsuario(Long idUsuario, UsuarioRequest request) throws UsuarioNoEncontradoException {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(UsuarioNoEncontradoException::new);

        copiarDatos(usuario, request);
        return usuarioRepository.save(usuario);
    }

    public void deleteUsuario(Long idUsuario) throws UsuarioNoEncontradoException {
        if (!usuarioRepository.existsById(idUsuario))
            throw new UsuarioNoEncontradoException();

        usuarioRepository.deleteById(idUsuario);
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
