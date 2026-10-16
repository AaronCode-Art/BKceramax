package com.ceramax.api.service.acceso;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.request.ClienteUpdateRequest;
import com.ceramax.api.dto.request.ActualizarDatosEntregaClienteRequest;
import com.ceramax.api.dto.request.BuscarClienteRegistroRequest;
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
    void actualizaLosDatosDeEntregaDelClienteAutenticado() {
        Cliente cliente = new Cliente();
        cliente.setEmail("cliente@example.com");
        cliente.setActivo(true);
        when(clienteRepository.findByEmail("cliente@example.com")).thenReturn(Optional.of(cliente));
        when(clienteRepository.saveAndFlush(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertSame(response, service.actualizarMisDatosEntrega(new ActualizarDatosEntregaClienteRequest(
            "912345678", "Arequipa", "Arequipa", "Yanahuara", "Calle Nueva 456",
            "04013", "Puerta azul", "040115"
        )));

        assertEquals("912345678", cliente.getTelefono());
        assertEquals("Calle Nueva 456", cliente.getDireccion());
        assertEquals("Yanahuara", cliente.getDistrito());
        verify(clienteRepository).saveAndFlush(cliente);
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

    @Test
    void encuentraDatosDeClienteActivoSinCuentaParaPrecargarRegistro() {
        Cliente cliente = new Cliente();
        cliente.setActivo(true);
        cliente.setPasswordHash(null);
        when(clienteRepository.findByTipoDocumentoAndNumeroDocumento("DNI", "12345678"))
            .thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertEquals(Optional.of(response), service.buscarDatosParaRegistro(
            new BuscarClienteRegistroRequest("DNI", "12345678")
        ));
    }

    @Test
    void noPermiteCompletarRegistroDeClienteQueYaTieneCuenta() {
        Cliente cliente = new Cliente();
        cliente.setActivo(true);
        cliente.setPasswordHash("existing-hash");
        when(clienteRepository.findByTipoDocumentoAndNumeroDocumento("DNI", "12345678"))
            .thenReturn(Optional.of(cliente));

        assertThrows(ResourceNotFoundException.class, () -> service.completarRegistro(registrationRequest()));
        verify(clienteRepository, never()).save(cliente);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void completaRegistroDeClientePreexistenteYGuardaSuContrasena() {
        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setActivo(true);
        cliente.setTipoDocumento("DNI");
        cliente.setNumeroDocumento("12345678");
        when(clienteRepository.findByTipoDocumentoAndNumeroDocumento("DNI", "12345678"))
            .thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("ana@example.com", cliente.getId()))
            .thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertSame(response, service.completarRegistro(registrationRequest()));
        assertEquals("encoded-password", cliente.getPasswordHash());
        assertEquals("ana@example.com", cliente.getEmail());
        verify(clienteRepository).save(cliente);
    }

    private ClienteUpdateRequest registrationRequest() {
        return new ClienteUpdateRequest(
            "DNI", "12345678", "Ana", "Prueba", "ANA@example.com",
            "password123", "987654321", "Lima", "Lima", "Miraflores",
            "Av. Principal 123", "15074", "Timbre 2", "150122", null
        );
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
