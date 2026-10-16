package com.ceramax.api.controller.acceso;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.request.ClienteUpdateRequest;
import com.ceramax.api.dto.request.ActualizarDatosEntregaClienteRequest;
import com.ceramax.api.dto.request.BuscarClienteRegistroRequest;
import com.ceramax.api.dto.response.ClienteResponse;
import com.ceramax.api.service.acceso.ClienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/v1")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/clientes")
    public ResponseEntity<Page<ClienteResponse>> listar(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))
        );
        return ResponseEntity.ok(clienteService.listarPagina(pageable));
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

    @PreAuthorize("hasRole('CLIENTE')")
    @PatchMapping("/clientes/mio/datos-entrega")
    public ResponseEntity<ClienteResponse> actualizarMisDatosEntrega(
        @Valid @RequestBody ActualizarDatosEntregaClienteRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizarMisDatosEntrega(request));
    }

    @PostMapping("/clientes")
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.registrar(request));
    }

    @PostMapping("/clientes/registro/buscar")
    public ResponseEntity<ClienteResponse> buscarDatosParaRegistro(@Valid @RequestBody BuscarClienteRegistroRequest request) {
        Optional<ClienteResponse> cliente = clienteService.buscarDatosParaRegistro(request);
        return cliente.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/clientes/registro/completar")
    public ResponseEntity<ClienteResponse> completarRegistro(
        @Valid @RequestBody ClienteUpdateRequest request
    ) {
        return ResponseEntity.ok(clienteService.completarRegistro(request));
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
