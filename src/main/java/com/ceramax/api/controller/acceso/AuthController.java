package com.ceramax.api.controller.acceso;

import com.ceramax.api.dto.request.LoginRequest;
import com.ceramax.api.dto.request.RestablecerContrasenaClienteRequest;
import com.ceramax.api.dto.request.SolicitudRecuperacionClienteRequest;
import com.ceramax.api.dto.response.AuthResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.service.acceso.AuthService;
import com.ceramax.api.service.acceso.RecuperacionContrasenaClienteService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final RecuperacionContrasenaClienteService recuperacionService;

    public AuthController(AuthService authService, RecuperacionContrasenaClienteService recuperacionService) {
        this.authService = authService;
        this.recuperacionService = recuperacionService;
    }

    @PostMapping("/auth/staff/login")
    public ResponseEntity<AuthResponse> loginStaff(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginStaff(request));
    }

    @PostMapping("/auth/cliente/login")
    public ResponseEntity<AuthResponse> loginCliente(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginCliente(request));
    }

    @PostMapping("/auth/cliente/recuperacion")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(
        @Valid @RequestBody SolicitudRecuperacionClienteRequest request
    ) {
        return ResponseEntity.ok(Map.of("message", recuperacionService.solicitarCodigo(request.identificador())));
    }

    @PostMapping("/auth/cliente/restablecer-contrasena")
    public ResponseEntity<Void> restablecerContrasena(
        @Valid @RequestBody RestablecerContrasenaClienteRequest request
    ) {
        if (!recuperacionService.restablecer(request)) {
            throw new BusinessException("El código no es válido o venció. Solicita uno nuevo.");
        }
        return ResponseEntity.noContent().build();
    }
}
