package com.uade.tpo.marketplace.controllers.fotos;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class FotoUploadRequest {
    private MultipartFile file;
    private Long idProducto;
}
