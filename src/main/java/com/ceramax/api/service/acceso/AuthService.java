package com.ceramax.api.service.acceso;

import com.ceramax.api.dto.request.LoginRequest;
import com.ceramax.api.dto.response.AuthResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UsuarioRepository usuarioRepository,
        ClienteRepository clienteRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse loginStaff(LoginRequest request) {
        Usuario usuario = usuarioRepository.findWithRolByEmail(request.email())
            .orElseThrow(() -> new BusinessException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(usuario.getActivo())
            || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new BusinessException("Credenciales inválidas");
        }

        String role = usuario.getRol() != null ? usuario.getRol().getCodigo() : "USER";
        String token = jwtService.generateToken(usuario.getEmail(), role);

        return new AuthResponse(token, "staff", usuario.getEmail(), usuario.getNombres(), usuario.getApellidos(), role);
    }

    public AuthResponse loginCliente(LoginRequest request) {
        Cliente cliente = clienteRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(cliente.getActivo())
            || cliente.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), cliente.getPasswordHash())) {
            throw new BusinessException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(cliente.getEmail(), "CLIENTE");
        return new AuthResponse(token, "cliente", cliente.getEmail(), cliente.getNombres(), cliente.getApellidos(), "CLIENTE");
    }
}
