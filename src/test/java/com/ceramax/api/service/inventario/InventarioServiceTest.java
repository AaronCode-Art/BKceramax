package com.ceramax.api.service.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.response.MovimientoInventarioResponse;
import com.ceramax.api.dto.response.ReservaInventarioResponse;
import com.ceramax.api.observer.StockBajoEventPublisher;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.inventario.InventarioConsultaRepository;
import com.ceramax.api.repository.inventario.InventarioOperacionRepository;
import com.ceramax.api.repository.inventario.MovimientoInventarioProjection;
import com.ceramax.api.repository.inventario.ReservaInventarioProjection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class InventarioServiceTest {

    private final InventarioConsultaRepository consultaRepository = mock(InventarioConsultaRepository.class);
    private InventarioService inventarioService;

    @BeforeEach
    void setUp() {
        inventarioService = new InventarioService(
            consultaRepository,
            mock(InventarioOperacionRepository.class),
            mock(UsuarioRepository.class),
            mock(ApplicationEventPublisher.class),
            mock(StockBajoEventPublisher.class)
        );
    }

    @Test
    void convierteFechaInstantAlListarMovimientos() {
        MovimientoInventarioProjection movimiento = mock(MovimientoInventarioProjection.class);
        when(movimiento.getCreatedAt()).thenReturn(Instant.parse("2026-10-05T18:51:35Z"));
        when(consultaRepository.listarMovimientos()).thenReturn(List.of(movimiento));

        List<MovimientoInventarioResponse> resultado = inventarioService.listarMovimientos();

        assertEquals(
            OffsetDateTime.parse("2026-10-05T18:51:35Z"),
            resultado.getFirst().fecha()
        );
    }

    @Test
    void convierteFechasInstantAlListarReservas() {
        ReservaInventarioProjection reserva = mock(ReservaInventarioProjection.class);
        when(reserva.getFechaPedido()).thenReturn(Instant.parse("2026-10-05T18:00:00Z"));
        when(reserva.getFechaReserva()).thenReturn(Instant.parse("2026-10-05T18:30:00Z"));
        when(consultaRepository.listarReservasActivas()).thenReturn(List.of(reserva));

        List<ReservaInventarioResponse> resultado = inventarioService.listarReservasActivas();

        assertEquals(OffsetDateTime.parse("2026-10-05T18:00:00Z"), resultado.getFirst().fechaPedido());
        assertEquals(OffsetDateTime.parse("2026-10-05T18:30:00Z"), resultado.getFirst().fechaReserva());
    }
}
