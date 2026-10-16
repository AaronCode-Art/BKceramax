package com.ceramax.api.mappers.venta;

import com.ceramax.api.dto.response.DatosEntregaResponse;
import com.ceramax.api.dto.response.DetallePedidoResponse;
import com.ceramax.api.dto.response.PagoResponse;
import com.ceramax.api.dto.response.PedidoEstadoHistorialResponse;
import com.ceramax.api.dto.response.PedidoResponse;
import com.ceramax.api.dto.response.PedidoResumenResponse;
import com.ceramax.api.model.venta.DetallePedido;
import com.ceramax.api.model.venta.Pago;
import com.ceramax.api.model.venta.Pedido;
import com.ceramax.api.repository.venta.PedidoEstadoHistorialProjection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper {

    public PedidoResponse toResponse(
        Pedido pedido,
        List<DetallePedido> lineas,
        List<Pago> pagosDelPedido,
        String estadoNombre
    ) {
        List<DetallePedidoResponse> detalles = lineas.stream()
            .map(detalle -> new DetallePedidoResponse(
                detalle.getId(),
                detalle.getProducto() == null ? null : detalle.getProducto().getId(),
                detalle.getProductoCodigo(),
                detalle.getProductoNombre(),
                detalle.getCategoriaNombre(),
                detalle.getProductoImagenUrl(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getDescuentoPorcentaje(),
                detalle.getSubtotal(),
                detalle.getEstadoStock()
            ))
            .toList();
        List<PagoResponse> pagos = pagosDelPedido.stream()
            .map(pago -> new PagoResponse(
                pago.getId(),
                pago.getCodigo(),
                pago.getMetodoPago(),
                pago.getEstado(),
                pago.getMonto(),
                pago.getFechaPago()
            ))
            .toList();
        return new PedidoResponse(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getCanal(),
            pedido.getTipoEntrega(),
            pedido.getEstado().getCodigo(),
            estadoNombre,
            pedido.getLogistica() == null ? null : pedido.getLogistica().getId(),
            pedido.getLogisticaNombre(),
            pedido.getDelivery() == null ? null : pedido.getDelivery().getId(),
            pedido.getDeliveryNombre(),
            pedido.getTipoComprobante(),
            pedido.getSerie(),
            pedido.getNumeroComprobante(),
            pedido.getRuc(),
            pedido.getRazonSocial(),
            pedido.getSubtotal(),
            pedido.getDescuentoCupon(),
            pedido.getCostoEnvio(),
            pedido.getIgv(),
            pedido.getTotal(),
            pedido.getTieneReserva(),
            pedido.getDespachado(),
            pedido.getCreatedAt(),
            detalles,
            pagos,
            toDatosEntrega(pedido)
        );
    }

    public PedidoResumenResponse toSummary(Pedido pedido, String estadoNombre) {
        return new PedidoResumenResponse(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getCanal(),
            pedido.getTipoEntrega(),
            pedido.getEstado().getCodigo(),
            estadoNombre,
            pedido.getTipoComprobante(),
            pedido.getSerie(),
            pedido.getNumeroComprobante(),
            pedido.getTotal(),
            pedido.getCreatedAt(),
            toDatosEntrega(pedido)
        );
    }

    public PedidoEstadoHistorialResponse toHistory(PedidoEstadoHistorialProjection event) {
        return new PedidoEstadoHistorialResponse(
            event.getId(),
            event.getEstadoAnteriorCodigo(),
            event.getEstadoCodigo(),
            event.getAccion(),
            event.getUsuarioId(),
            event.getUsuarioNombre(),
            event.getCreadoEn()
        );
    }

    private DatosEntregaResponse toDatosEntrega(Pedido pedido) {
        return new DatosEntregaResponse(
            pedido.getClienteNombre(),
            pedido.getClienteTelefono(),
            pedido.getEnvioDepartamento(),
            pedido.getEnvioProvincia(),
            pedido.getEnvioDistrito(),
            pedido.getEnvioDireccion(),
            pedido.getEnvioCodigoPostal(),
            pedido.getEnvioReferencia(),
            pedido.getSucursalNombre(),
            pedido.getSucursalDireccion()
        );
    }
}
