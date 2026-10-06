package com.ceramax.api.service.venta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.CrearPedidoWebRequest;
import com.ceramax.api.dto.response.PedidoResponse;
import com.ceramax.api.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PagoSimuladoServiceTest {

    private final PedidoService pedidoService = mock(PedidoService.class);

    @Test
    void rejectsSimulationWhenDisabled() {
        PagoSimuladoService service = new PagoSimuladoService(pedidoService, false);

        assertThrows(BusinessException.class, () -> service.confirmar(request()));
        verifyNoInteractions(pedidoService);
    }

    @Test
    void delegatesCheckoutWhenSimulationIsEnabled() {
        PagoSimuladoService service = new PagoSimuladoService(pedidoService, true);
        PedidoResponse order = new PedidoResponse(
            UUID.randomUUID(),
            "PED-000001",
            "WEB",
            "RECOJO_TIENDA",
            "PREPARACION",
            "En preparación",
            null,
            null,
            null,
            null,
            "BOLETA",
            null,
            null,
            BigDecimal.TEN,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.TEN,
            true,
            false,
            null,
            List.of(),
            List.of(),
            null
        );
        when(pedidoService.confirmarCheckoutWebSimulado(request())).thenReturn(order);

        assertEquals(order, service.confirmar(request()));
        verify(pedidoService).confirmarCheckoutWebSimulado(request());
    }

    private CrearPedidoWebRequest request() {
        return new CrearPedidoWebRequest(
            "RECOJO_TIENDA",
            "BOLETA",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "SIMULADO",
            "YAPE",
            List.of()
        );
    }
}
