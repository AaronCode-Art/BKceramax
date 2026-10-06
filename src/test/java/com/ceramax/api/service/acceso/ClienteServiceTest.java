package com.ceramax.api.service.acceso;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.response.ClienteResponse;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.acceso.ClienteMapper;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.repository.acceso.ClienteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class ClienteServiceTest {

    private final ClienteRepository clienteRepository = org.mockito.Mockito.mock(ClienteRepository.class);
    private final ClienteMapper clienteMapper = org.mockito.Mockito.mock(ClienteMapper.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private final ClienteService service = new ClienteService(clienteRepository, clienteMapper, passwordEncoder);
    private final ClienteResponse response = new ClienteResponse(
        UUID.randomUUID(),
        "CLI-0001",
        "Ana",
        "Ceramista",
        "cliente@example.com",
        "DNI",
        "12345678",
        "987654321",
        "Lima",
        "Lima",
        "Miraflores",
        "Av. Principal 123",
        "15074",
        "Timbre 2",
        "150122",
        true,
        null,
        null
    );

    @BeforeEach
    void autenticarCliente() {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("cliente@example.com", null, List.of())
        );
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtieneElPerfilDelClienteAutenticado() {
        Cliente cliente = new Cliente();
        cliente.setEmail("cliente@example.com");
        cliente.setActivo(true);
        when(clienteRepository.findByEmail("cliente@example.com")).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertSame(response, service.obtenerMiPerfil());
        verify(clienteRepository).findByEmail("cliente@example.com");
    }

    @Test
    void noExponeElPerfilDeUnClienteDesactivado() {
        Cliente cliente = new Cliente();
        cliente.setActivo(false);
        when(clienteRepository.findByEmail("cliente@example.com")).thenReturn(Optional.of(cliente));

        assertThrows(ResourceNotFoundException.class, service::obtenerMiPerfil);
        verify(clienteMapper, never()).toResponse(cliente);
    }

    @Test
    void rechazaCorreoDuplicadoConMensajeClaro() {
        ClienteRequest request = request();
        when(clienteRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.registrar(request));

        assertEquals("El correo electrónico ya existe.", exception.getMessage());
        verify(clienteRepository).existsByEmailIgnoreCase("ana@example.com");
        verifyNoInteractions(clienteMapper, passwordEncoder);
    }

    @Test
    void rechazaDocumentoDuplicadoConMensajeClaro() {
        ClienteRequest request = request();
        when(clienteRepository.existsByNumeroDocumentoAndTipoDocumento("12345678", "DNI")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.registrar(request));

        assertEquals("El número de documento ya existe para ese tipo de documento.", exception.getMessage());
        verify(clienteRepository).existsByNumeroDocumentoAndTipoDocumento("12345678", "DNI");
        verifyNoInteractions(clienteMapper, passwordEncoder);
    }

    private ClienteRequest request() {
        return new ClienteRequest(
            "DNI",
            "12345678",
            "Ana",
            "Prueba",
            "ANA@example.com",
            "password123",
            "987654321",
            "Lima",
            "Lima",
            "Miraflores",
            "Av. Principal 123",
            "15074",
            "Timbre 2",
            "150122"
        );
    }
}
