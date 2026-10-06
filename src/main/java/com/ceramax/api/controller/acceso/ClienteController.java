package com.ceramax.api.controller.acceso;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.request.ClienteUpdateRequest;
import com.ceramax.api.dto.response.ClienteResponse;
import com.ceramax.api.service.acceso.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/clientes/{id}")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/clientes/mio")
    public ResponseEntity<ClienteResponse> obtenerMiPerfil() {
        return ResponseEntity.ok(clienteService.obtenerMiPerfil());
    }

    @PostMapping("/clientes")
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.registrar(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/clientes/{id}")
    public ResponseEntity<ClienteResponse> actualizar(
        @PathVariable UUID id,
        @Valid @RequestBody ClienteUpdateRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }
}
