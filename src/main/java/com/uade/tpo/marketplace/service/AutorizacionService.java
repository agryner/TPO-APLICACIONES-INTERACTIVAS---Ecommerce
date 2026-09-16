package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.exceptions.AccesoDenegadoException;
import com.uade.tpo.marketplace.exceptions.CuentaInactivaException;
import com.uade.tpo.marketplace.exceptions.OperacionAjenaException;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

public interface AutorizacionService {
    void validarActivo(Long idUsuario)
            throws UsuarioNoEncontradoException, CuentaInactivaException;

    void validarDuenio(Long idSolicitante, Long idDuenio)
            throws OperacionAjenaException, UsuarioNoEncontradoException, CuentaInactivaException;

    boolean esAdmin(Long idUsuario);

    void validarQuePuedaComerciar(Long idSolicitante)
            throws UsuarioNoEncontradoException, RolNoComerciaException;

    void validarAdmin(Long idUsuario)
            throws UsuarioNoEncontradoException, AccesoDenegadoException;
}
