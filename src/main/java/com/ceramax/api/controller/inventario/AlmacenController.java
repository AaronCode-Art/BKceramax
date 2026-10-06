package com.ceramax.api.controller.inventario;

import com.ceramax.api.dto.request.AlmacenRequest;
import com.ceramax.api.dto.response.AlmacenResponse;
import com.ceramax.api.service.inventario.AlmacenService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventario/almacenes")
public class AlmacenController {

    private final AlmacenService almacenService;

    public AlmacenController(AlmacenService almacenService) {
        this.almacenService = almacenService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<List<AlmacenResponse>> listar() {
        return ResponseEntity.ok(almacenService.listarActivos());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlmacenResponse> crear(@Valid @RequestBody AlmacenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(almacenService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlmacenResponse> actualizar(
        @PathVariable UUID id,
        @Valid @RequestBody AlmacenRequest request
    ) {
        return ResponseEntity.ok(almacenService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        almacenService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
