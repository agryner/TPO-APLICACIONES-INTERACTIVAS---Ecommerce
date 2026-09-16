package com.uade.tpo.marketplace.service;

import com.uade.tpo.marketplace.controllers.dashboard.DashboardResponse;
import com.uade.tpo.marketplace.exceptions.RolNoComerciaException;
import com.uade.tpo.marketplace.exceptions.UsuarioNoEncontradoException;

public interface DashboardService {
    DashboardResponse getDelVendedor(Long idVendedor)
            throws UsuarioNoEncontradoException, RolNoComerciaException;
}
