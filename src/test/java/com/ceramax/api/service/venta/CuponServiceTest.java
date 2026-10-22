package com.ceramax.api.service.venta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.model.venta.Cupon;
import com.ceramax.api.repository.venta.CuponRepository;
import com.ceramax.api.repository.venta.PedidoRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CuponServiceTest {

    private final CuponRepository repository = mock(CuponRepository.class);
    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final CuponService service = new CuponService(repository, pedidoRepository);

    @Test
    void calculaPorcentajeSobreElSubtotal() {
        Cupon coupon = cupon("CERAMAX10", "PORCENTAJE", "10", null);
        when(repository.findByCodigo("CERAMAX10")).thenReturn(Optional.of(coupon));

        BigDecimal discount = service.calcularDescuento("ceramax10", new BigDecimal("250.00"), false, UUID.randomUUID());

        assertEquals(new BigDecimal("25.00"), discount);
    }

    @Test
    void limitaDescuentoFijoAlSubtotal() {
        Cupon coupon = cupon("FIJO", "MONTO_FIJO", "50", null);
        when(repository.findByCodigo("FIJO")).thenReturn(Optional.of(coupon));

        BigDecimal discount = service.calcularDescuento("FIJO", new BigDecimal("12.00"), false, UUID.randomUUID());

        assertEquals(new BigDecimal("12.00"), discount);
    }

    @Test
    void rechazaCuponFueraDeVigencia() {
        Cupon expired = cupon("VENCIDO", "PORCENTAJE", "10", null);
        expired.setFechaFin(OffsetDateTime.now().minusMinutes(1));
        when(repository.findByCodigo("VENCIDO")).thenReturn(Optional.of(expired));

        assertThrows(BusinessException.class,
            () -> service.calcularDescuento("VENCIDO", new BigDecimal("100.00"), false, UUID.randomUUID()));
    }

    @Test
    void rechazaCuponQueAlcanzoElMaximoDeUsos() {
        Cupon coupon = cupon("LIMITADO", "PORCENTAJE", "10", 5);
        when(repository.findByCodigo("LIMITADO")).thenReturn(Optional.of(coupon));
        when(pedidoRepository.countByCupon_Id(coupon.getId())).thenReturn(5L);

        assertThrows(BusinessException.class,
            () -> service.calcularDescuento("LIMITADO", new BigDecimal("100.00"), false, UUID.randomUUID()));
    }

    @Test
    void limitaLosUsosPorCliente() {
        Cupon coupon = cupon("UNO", "MONTO_FIJO", "5", null);
        UUID customerId = UUID.randomUUID();
        when(repository.findByCodigo("UNO")).thenReturn(Optional.of(coupon));
        when(pedidoRepository.countByCupon_IdAndCliente_Id(coupon.getId(), customerId))
            .thenReturn(1L);

        assertThrows(BusinessException.class,
            () -> service.calcularDescuento("uno", new BigDecimal("100.00"), false, customerId));
    }

    private static Cupon cupon(String code, String type, String amount, Integer useLimit) {
        Cupon coupon = new Cupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCodigo(code);
        coupon.setTipoDescuento(type);
        coupon.setValor(new BigDecimal(amount));
        coupon.setMontoMinimo(BigDecimal.ZERO);
        coupon.setUsosMaximos(useLimit);
        coupon.setLimitePorCliente(1);
        coupon.setFechaInicio(OffsetDateTime.now().minusDays(1));
        coupon.setFechaFin(OffsetDateTime.now().plusDays(1));
        coupon.setDescripcion("Oferta de prueba");
        coupon.setActivo(true);
        return coupon;
    }
}
