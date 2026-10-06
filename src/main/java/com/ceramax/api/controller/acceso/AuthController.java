package com.ceramax.api.controller.acceso;

import com.ceramax.api.dto.request.LoginRequest;
import com.ceramax.api.dto.response.AuthResponse;
import com.ceramax.api.service.acceso.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/staff/login")
    public ResponseEntity<AuthResponse> loginStaff(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginStaff(request));
    }

    @PostMapping("/auth/cliente/login")
    public ResponseEntity<AuthResponse> loginCliente(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginCliente(request));
    }
}
