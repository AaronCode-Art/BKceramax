package com.ceramax.api.service.acceso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.LoginRequest;
import com.ceramax.api.dto.response.AuthResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Rol;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final UsuarioRepository usuarioRepository = org.mockito.Mockito.mock(UsuarioRepository.class);
    private final ClienteRepository clienteRepository = org.mockito.Mockito.mock(ClienteRepository.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private final JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, clienteRepository, passwordEncoder, jwtService);
    }

    @Test
    void rechazaInicioDeSesionDePersonalDesactivado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("staff@example.test");
        usuario.setActivo(false);
        usuario.setPasswordHash("encoded-password");
        when(usuarioRepository.findWithRolByEmail("staff@example.test")).thenReturn(Optional.of(usuario));

        assertThrows(
            BusinessException.class,
            () -> authService.loginStaff(new LoginRequest("staff@example.test", "password"))
        );

        verify(passwordEncoder, never()).matches("password", "encoded-password");
        verify(jwtService, never()).generateToken("staff@example.test", "USER");
    }

    @Test
    void rechazaInicioDeSesionDeClienteDesactivado() {
        Cliente cliente = new Cliente();
        cliente.setEmail("client@example.test");
        cliente.setActivo(false);
        cliente.setPasswordHash("encoded-password");
        when(clienteRepository.findByEmail("client@example.test")).thenReturn(Optional.of(cliente));

        assertThrows(
            BusinessException.class,
            () -> authService.loginCliente(new LoginRequest("client@example.test", "password"))
        );

        verify(passwordEncoder, never()).matches("password", "encoded-password");
        verify(jwtService, never()).generateToken("client@example.test", "CLIENTE");
    }

    @Test
    void permiteInicioDeSesionDePersonalActivo() {
        Usuario usuario = new Usuario();
        usuario.setEmail("staff@example.test");
        usuario.setNombres("Ana");
        usuario.setApellidos("Prueba");
        usuario.setActivo(true);
        usuario.setPasswordHash("encoded-password");
        Rol rol = new Rol();
        rol.setCodigo("ADMIN");
        usuario.setRol(rol);
        when(usuarioRepository.findWithRolByEmail("staff@example.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("staff@example.test", "ADMIN")).thenReturn("token");

        AuthResponse response = authService.loginStaff(new LoginRequest("staff@example.test", "password"));

        assertEquals("token", response.token());
        assertEquals("ADMIN", response.rol());
        verify(jwtService).generateToken("staff@example.test", "ADMIN");
    }

    @Test
    void permiteInicioDeSesionDeClienteActivo() {
        Cliente cliente = new Cliente();
        cliente.setEmail("client@example.test");
        cliente.setNombres("Ana");
        cliente.setApellidos("Prueba");
        cliente.setActivo(true);
        cliente.setPasswordHash("encoded-password");
        when(clienteRepository.findByEmail("client@example.test")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("client@example.test", "CLIENTE")).thenReturn("token");

        AuthResponse response = authService.loginCliente(new LoginRequest("client@example.test", "password"));

        assertEquals("token", response.token());
        assertEquals("CLIENTE", response.rol());
        verify(jwtService).generateToken("client@example.test", "CLIENTE");
    }
}
