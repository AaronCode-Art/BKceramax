package com.ceramax.api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Rol;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class CeramaxUserDetailsServiceTest {

    private final UsuarioRepository usuarioRepository = org.mockito.Mockito.mock(UsuarioRepository.class);
    private final ClienteRepository clienteRepository = org.mockito.Mockito.mock(ClienteRepository.class);
    private CeramaxUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CeramaxUserDetailsService(usuarioRepository, clienteRepository);
    }

    @Test
    void personalDesactivadoSeRepresentaComoCuentaDeshabilitada() {
        Usuario usuario = new Usuario();
        usuario.setEmail("staff@example.test");
        usuario.setPasswordHash("encoded-password");
        usuario.setActivo(false);
        Rol rol = new Rol();
        rol.setCodigo("ADMIN");
        usuario.setRol(rol);
        when(usuarioRepository.findWithRolByEmail("staff@example.test")).thenReturn(Optional.of(usuario));

        UserDetails details = userDetailsService.loadUserByUsername("staff@example.test");

        assertFalse(details.isEnabled());
    }

    @Test
    void clienteDesactivadoSeRepresentaComoCuentaDeshabilitada() {
        Cliente cliente = new Cliente();
        cliente.setEmail("client@example.test");
        cliente.setPasswordHash("encoded-password");
        cliente.setActivo(false);
        when(clienteRepository.findByEmail("client@example.test")).thenReturn(Optional.of(cliente));

        UserDetails details = userDetailsService.loadUserByUsername("client@example.test");

        assertFalse(details.isEnabled());
    }

    @Test
    void cuentaActivaPermaneceHabilitada() {
        Cliente cliente = new Cliente();
        cliente.setEmail("client@example.test");
        cliente.setPasswordHash("encoded-password");
        cliente.setActivo(true);
        when(clienteRepository.findByEmail("client@example.test")).thenReturn(Optional.of(cliente));

        UserDetails details = userDetailsService.loadUserByUsername("client@example.test");

        assertTrue(details.isEnabled());
    }

    @Test
    void cargaElRolDelPersonalParaAutorizarSolicitudesJwt() {
        Usuario usuario = new Usuario();
        usuario.setEmail("staff@example.test");
        usuario.setPasswordHash("encoded-password");
        usuario.setActivo(true);
        Rol rol = new Rol();
        rol.setCodigo("ADMIN");
        usuario.setRol(rol);
        when(usuarioRepository.findWithRolByEmail("staff@example.test")).thenReturn(Optional.of(usuario));

        UserDetails details = userDetailsService.loadUserByUsername("staff@example.test");

        assertTrue(details.isEnabled());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }
}
