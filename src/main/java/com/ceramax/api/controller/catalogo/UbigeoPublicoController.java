package com.ceramax.api.controller.catalogo;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/ubigeos")
public class UbigeoPublicoController {

    private final byte[] ubigeos;

    public UbigeoPublicoController() {
        try (InputStream input = new ClassPathResource("ubigeo-peru.json").getInputStream()) {
            this.ubigeos = input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar el catálogo UBIGEO", exception);
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> listar() {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(ubigeos);
    }
}
