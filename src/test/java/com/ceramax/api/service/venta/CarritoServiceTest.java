package com.ceramax.api.service.venta;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.repository.acceso.ClienteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CarritoServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final CarritoService carritoService = new CarritoService(jdbcTemplate, clienteRepository);

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void consultarCarritoInexistenteNoIntentaCrearRegistro() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setActivo(true);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("cliente@example.com", null)
        );
        when(clienteRepository.findByEmail("cliente@example.com")).thenReturn(Optional.of(cliente));
        when(jdbcTemplate.query(
            eq("SELECT id FROM carrito WHERE id_cliente = ?"),
            any(RowMapper.class),
            eq(clienteId)
        )).thenReturn(List.of());

        assertTrue(carritoService.obtener().isEmpty());

        verify(jdbcTemplate, never()).queryForObject(
            org.mockito.ArgumentMatchers.contains("INSERT INTO carrito"),
            eq(UUID.class),
            eq(clienteId)
        );
    }
}
