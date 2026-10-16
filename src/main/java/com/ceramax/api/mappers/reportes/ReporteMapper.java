package com.ceramax.api.mappers.reportes;

import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.InventarioReporteResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.PagoResumenResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.ProductoMasVendidoResponse;
import com.ceramax.api.repository.reportes.ReporteRepository.InventarioActual;
import com.ceramax.api.repository.reportes.ReporteRepository.ProductoMasVendido;
import com.ceramax.api.repository.reportes.ReporteRepository.ResumenPagos;
import org.springframework.stereotype.Component;

@Component
public class ReporteMapper {

    public ProductoMasVendidoResponse toProductoMasVendido(ProductoMasVendido producto) {
        return new ProductoMasVendidoResponse(
            producto.productoId(),
            producto.productoCodigo(),
            producto.productoNombre(),
            producto.unidadesVendidas(),
            producto.importeVendido()
        );
    }

    public InventarioReporteResponse toInventario(InventarioActual item) {
        return new InventarioReporteResponse(
            item.almacenId(),
            item.almacenCodigo(),
            item.almacenNombre(),
            item.productoId(),
            item.productoCodigo(),
            item.productoNombre(),
            item.categoriaNombre(),
            item.stockFisico(),
            item.stockReservado(),
            item.stockDisponible(),
            item.stockMinimo(),
            item.stockBajo()
        );
    }

    public PagoResumenResponse toPagoResumen(ResumenPagos pago) {
        return new PagoResumenResponse(
            pago.metodoPago(),
            pago.estado(),
            pago.cantidadPagos(),
            pago.montoTotal()
        );
    }
}
