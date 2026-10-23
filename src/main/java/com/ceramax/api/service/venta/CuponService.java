package com.ceramax.api.service.venta;

import com.ceramax.api.dto.request.CuponRequest;
import com.ceramax.api.dto.response.CuponAnuncioResponse;
import com.ceramax.api.dto.response.CuponResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.model.venta.Cupon;
import com.ceramax.api.repository.venta.CuponRepository;
import com.ceramax.api.repository.venta.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CuponService {

    private final CuponRepository cuponRepository;
    private final PedidoRepository pedidoRepository;

    public CuponService(CuponRepository cuponRepository, PedidoRepository pedidoRepository) {
        this.cuponRepository = cuponRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<CuponResponse> listar() {
        return cuponRepository.findAllByOrderByFechaInicioDesc().stream()
            .map(this::respuesta)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CuponAnuncioResponse> listarAnunciosVigentes() {
        return cuponRepository.findCurrentAnnouncements().stream()
            .map(cupon -> new CuponAnuncioResponse(
                cupon.getCodigo(),
                cupon.getTipoDescuento(),
                cupon.getValor(),
                cupon.getMontoMinimo(),
                cupon.getLimitePorCliente(),
                cupon.getFechaFin(),
                tituloAnuncio(cupon),
                cupon.getDescripcion()
            ))
            .toList();
    }

    public CuponResponse crear(CuponRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (cuponRepository.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un cupón con ese código");
        }
        Cupon cupon = new Cupon();
        aplicar(cupon, request, codigo);
        return respuesta(cuponRepository.save(cupon));
    }

    public CuponResponse actualizar(UUID id, CuponRequest request) {
        Cupon cupon = cuponRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cupón no encontrado"));
        String codigo = normalizarCodigo(request.codigo());
        if (cuponRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ConflictException("Ya existe un cupón con ese código");
        }
        aplicar(cupon, request, codigo);
        return respuesta(cuponRepository.save(cupon));
    }

    public void desactivar(UUID id) {
        Cupon cupon = cuponRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cupón no encontrado"));
        cupon.setActivo(false);
        cuponRepository.save(cupon);
    }

    public BigDecimal calcularDescuento(
        String codigoIngresado,
        BigDecimal subtotal,
        boolean bloquear,
        UUID clienteId
    ) {
        if (codigoIngresado == null || codigoIngresado.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String codigo = normalizarCodigo(codigoIngresado);
        Cupon cupon = buscar(codigo, bloquear);
        validarVigencia(cupon, clienteId);
        if (subtotal.compareTo(cupon.getMontoMinimo()) < 0) {
            throw new BusinessException(
                "El cupón requiere una compra mínima de S/ "
                    + cupon.getMontoMinimo().setScale(2, RoundingMode.HALF_UP)
            );
        }
        BigDecimal descuento = "PORCENTAJE".equals(cupon.getTipoDescuento())
            ? subtotal.multiply(cupon.getValor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : cupon.getValor();
        return descuento.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Cupon buscarParaPedido(String codigoIngresado) {
        return buscar(normalizarCodigo(codigoIngresado), false);
    }

    private Cupon buscar(String codigo, boolean bloquear) {
        return (bloquear
            ? cuponRepository.findByCodigoForUpdate(codigo)
            : cuponRepository.findByCodigo(codigo))
            .orElseThrow(() -> new BusinessException("El código de cupón no existe o ya no está disponible"));
    }

    private void validarVigencia(Cupon cupon, UUID clienteId) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (!Boolean.TRUE.equals(cupon.getActivo())
            || ahora.isBefore(cupon.getFechaInicio())
            || (cupon.getFechaFin() != null && ahora.isAfter(cupon.getFechaFin()))) {
            throw new BusinessException("El cupón no está vigente");
        }
        if (cupon.getCliente() != null
            && (clienteId == null || !cupon.getCliente().getId().equals(clienteId))) {
            throw new BusinessException("El cupón no está disponible para este cliente");
        }

        long usosTotales = pedidoRepository.countByCupon_Id(cupon.getId());
        if (cupon.getUsosMaximos() != null && usosTotales >= cupon.getUsosMaximos()) {
            throw new BusinessException("El cupón alcanzó su límite de usos");
        }
        if (clienteId != null
            && pedidoRepository.countByCupon_IdAndCliente_Id(cupon.getId(), clienteId)
                >= cupon.getLimitePorCliente()) {
            throw new BusinessException("Ya alcanzaste el límite de usos de este cupón");
        }
    }

    private void aplicar(Cupon cupon, CuponRequest request, String codigo) {
        if (request.venceEn() != null && !request.venceEn().isAfter(request.iniciaEn())) {
            throw new BusinessException("La fecha de vencimiento debe ser posterior al inicio");
        }
        if ("PORCENTAJE".equals(request.tipoDescuento())
            && request.valor().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("El descuento porcentual no puede superar el 100%");
        }
        cupon.setCodigo(codigo);
        cupon.setDescripcion(request.descripcion().trim());
        cupon.setTipoDescuento(request.tipoDescuento());
        cupon.setValor(request.valor().setScale(2, RoundingMode.HALF_UP));
        cupon.setMontoMinimo(request.compraMinima().setScale(2, RoundingMode.HALF_UP));
        cupon.setUsosMaximos(request.usoMaximo());
        cupon.setLimitePorCliente(request.limitePorCliente());
        cupon.setFechaInicio(request.iniciaEn());
        cupon.setFechaFin(request.venceEn());
        cupon.setActivo(request.activo());
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private CuponResponse respuesta(Cupon cupon) {
        return new CuponResponse(
            cupon.getId(),
            cupon.getCodigo(),
            cupon.getTipoDescuento(),
            cupon.getValor(),
            cupon.getMontoMinimo(),
            cupon.getUsosMaximos(),
            pedidoRepository.countByCupon_Id(cupon.getId()),
            cupon.getLimitePorCliente(),
            cupon.getFechaInicio(),
            cupon.getFechaFin(),
            tituloAnuncio(cupon),
            cupon.getDescripcion(),
            cupon.getActivo(),
            cupon.getActivo()
        );
    }

    private String tituloAnuncio(Cupon cupon) {
        return "Cupón " + cupon.getCodigo();
    }
}
