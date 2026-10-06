package com.ceramax.api.controller.catalogo;

import com.ceramax.api.dto.request.CategoriaRequest;
import com.ceramax.api.dto.response.CategoriaResponse;
import com.ceramax.api.service.catalogo.CategoriaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
@RequestMapping("/api/v1")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/catalogo/categorias")
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    @GetMapping("/catalogo/categorias/{id}")
    public ResponseEntity<CategoriaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    @PostMapping("/catalogo/categorias")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @PutMapping("/catalogo/categorias/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    @DeleteMapping("/catalogo/categorias/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        categoriaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
