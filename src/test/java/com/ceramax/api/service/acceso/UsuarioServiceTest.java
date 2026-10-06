package com.ceramax.api.service.acceso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.UsuarioRequest;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.mappers.acceso.UsuarioMapper;
import com.ceramax.api.repository.acceso.RolRepository;
import com.ceramax.api.repository.acceso.SucursalRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final RolRepository rolRepository = mock(RolRepository.class);
    private final SucursalRepository sucursalRepository = mock(SucursalRepository.class);
    private final UsuarioMapper usuarioMapper = mock(UsuarioMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final UsuarioService service = new UsuarioService(
        usuarioRepository,
        rolRepository,
        sucursalRepository,
        usuarioMapper,
        passwordEncoder,
        entityManager
    );

    @Test
    void rechazaCodigoExistenteConMensajeEspecificoAntesDeGuardar() {
        UsuarioRequest request = new UsuarioRequest(
            "  EMP-001  ",
            UUID.randomUUID(),
            null,
            "DNI",
            "12345678",
            "Nombre",
            "Apellido",
            "nombre@example.com",
            "password",
            null,
            true
        );
        when(usuarioRepository.existsByCodigo("EMP-001")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.crear(request));

        assertEquals("El código de personal ya existe.", exception.getMessage());
        verify(usuarioRepository).existsByCodigo("EMP-001");
        verifyNoInteractions(rolRepository, sucursalRepository, usuarioMapper, passwordEncoder, entityManager);
    }

    @Test
    void rechazaCorreoDuplicadoConMensajeClaro() {
        UsuarioRequest request = request("AB345678", "ana@example.com", "12345678");
        when(usuarioRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.crear(request));

        assertEquals("El correo electrónico ya existe.", exception.getMessage());
        verify(usuarioRepository).existsByEmailIgnoreCase("ana@example.com");
        verifyNoInteractions(rolRepository, sucursalRepository, usuarioMapper, passwordEncoder, entityManager);
    }

    @Test
    void rechazaNumeroDeDocumentoDuplicadoConMensajeClaro() {
        UsuarioRequest request = request("AB345678", "ana@example.com", "12345678");
        when(usuarioRepository.existsByNumeroDocumentoAndTipoDocumento("12345678", "DNI")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.crear(request));

        assertEquals("El número de documento ya existe para ese tipo de documento.", exception.getMessage());
        verify(usuarioRepository).existsByNumeroDocumentoAndTipoDocumento("12345678", "DNI");
        verifyNoInteractions(rolRepository, sucursalRepository, usuarioMapper, passwordEncoder, entityManager);
    }

    private UsuarioRequest request(String codigo, String email, String documento) {
        return new UsuarioRequest(
            codigo,
            UUID.randomUUID(),
            null,
            "DNI",
            documento,
            "Ana",
            "Prueba",
            email,
            "password",
            null,
            true
        );
    }
}
