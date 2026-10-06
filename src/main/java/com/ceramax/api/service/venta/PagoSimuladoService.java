package com.ceramax.api.service.venta;

import com.ceramax.api.dto.request.CrearPedidoWebRequest;
import com.ceramax.api.dto.response.PedidoResponse;
import com.ceramax.api.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PagoSimuladoService {

    private final PedidoService pedidoService;
    private final boolean habilitado;

    public PagoSimuladoService(
        PedidoService pedidoService,
        @Value("${app.checkout.simulado.enabled:false}") boolean habilitado
    ) {
        this.pedidoService = pedidoService;
        this.habilitado = habilitado;
    }

    public PedidoResponse confirmar(CrearPedidoWebRequest request) {
        if (!habilitado) {
            throw new BusinessException("El pago simulado está deshabilitado en este entorno");
        }
        return pedidoService.confirmarCheckoutWebSimulado(request);
    }
}
