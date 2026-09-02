package com.uade.tpo.marketplace.entity.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

/**
 * Se completa desde un formulario multipart/form-data, no desde un JSON:
 * por eso el controller lo recibe como @ModelAttribute y no como @RequestBody.
 *
 * Viaja del controller a FotoService, que lo convierte en una Foto.
 */
@Data
public class FotoUploadRequest {
    private MultipartFile file;
    private Long idProducto;
}
