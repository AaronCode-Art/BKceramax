package com.ceramax.api.factory;

import com.ceramax.api.dto.request.CrearPedidoWebRequest;
import com.ceramax.api.dto.response.ComprobanteDatos;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Sucursal;
import com.ceramax.api.model.venta.EstadoEnvio;
import com.ceramax.api.model.venta.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PedidoFactory {

    private final ComprobanteFactory comprobanteFactory;

    public PedidoFactory(ComprobanteFactory comprobanteFactory) {
        this.comprobanteFactory = comprobanteFactory;
    }

    public Pedido crearPedidoWeb(
        CrearPedidoWebRequest request,
        Cliente cliente,
        Sucursal sucursal,
        EstadoEnvio estadoInicial,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal igv,
        BigDecimal total
    ) {
        if (!"DELIVERY".equals(request.tipoEntrega()) && !"RECOJO_TIENDA".equals(request.tipoEntrega())) {
            throw new BusinessException("Tipo de entrega inválido para pedido web");
        }
        if (
            "DELIVERY".equals(request.tipoEntrega())
                && (isBlank(request.envioDireccion()) || isBlank(request.envioDistrito()))
        ) {
            throw new BusinessException("Para DELIVERY se requiere dirección y distrito");
        }

        ComprobanteDatos comprobante = comprobanteFactory.crear(
            request.tipoComprobante(),
            request.ruc(),
            request.razonSocial()
        );
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setSucursal(sucursal);
        pedido.setEstado(estadoInicial);
        pedido.setCanal("WEB");
        pedido.setTipoEntrega(request.tipoEntrega());
        pedido.setClienteTipoDocumento(cliente.getTipoDocumento());
        pedido.setClienteNumeroDocumento(cliente.getNumeroDocumento());
        pedido.setClienteNombre(cliente.getNombres() + " " + cliente.getApellidos());
        pedido.setClienteEmail(cliente.getEmail());
        pedido.setClienteTelefono(cliente.getTelefono());
        pedido.setEnvioDepartamento(request.envioDepartamento());
        pedido.setEnvioProvincia(request.envioProvincia());
        pedido.setEnvioDistrito(request.envioDistrito());
        pedido.setEnvioDireccion(request.envioDireccion());
        pedido.setEnvioCodigoPostal(request.envioCodigoPostal());
        pedido.setEnvioReferencia(request.envioReferencia());
        pedido.setEnvioIdUbigeo(request.envioIdUbigeo());
        pedido.setSucursalNombre(sucursal.getNombre());
        pedido.setSucursalDireccion(sucursal.getDireccion());
        pedido.setCostoEnvio(costoEnvio);
        pedido.setSubtotal(subtotal);
        pedido.setDescuentoCupon(BigDecimal.ZERO);
        pedido.setIgv(igv);
        pedido.setTotal(total);
        pedido.setTipoComprobante(comprobante.tipo());
        pedido.setRuc(comprobante.ruc());
        pedido.setRazonSocial(comprobante.razonSocial());
        pedido.setStockPendiente(false);
        pedido.setTieneReserva(false);
        pedido.setDespachado(false);
        return pedido;
    }

    public ComprobanteDatos validarPedidoPresencial(String tipoComprobante, String ruc, String razonSocial) {
        return comprobanteFactory.crear(tipoComprobante, ruc, razonSocial);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
