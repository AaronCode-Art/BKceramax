package com.ceramax.api.service.inventario;

import com.ceramax.api.dto.request.AjusteInventarioRequest;
import com.ceramax.api.dto.request.EntradaInventarioRequest;
import com.ceramax.api.dto.request.TrasladoInventarioRequest;
import com.ceramax.api.dto.response.AjusteCreadoResponse;
import com.ceramax.api.dto.response.AjusteInventarioResponse;
import com.ceramax.api.dto.response.InventarioResponse;
import com.ceramax.api.dto.response.MovimientoInventarioResponse;
import com.ceramax.api.dto.response.OperacionInventarioResponse;
import com.ceramax.api.dto.response.ReservaInventarioResponse;
import com.ceramax.api.dto.response.SolicitudEntradaResponse;
import com.ceramax.api.dto.response.StockDisponibleResponse;
import com.ceramax.api.dto.response.TrasladoCreadoResponse;
import com.ceramax.api.dto.response.TrasladoInventarioResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ForbiddenException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.observer.StockBajoEventPublisher;
import com.ceramax.api.observer.event.TrasladoRecibidoEvent;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.inventario.AjusteInventarioProjection;
import com.ceramax.api.repository.inventario.InventarioConsultaRepository;
import com.ceramax.api.repository.inventario.InventarioOperacionRepository;
import com.ceramax.api.repository.inventario.InventarioProjection;
import com.ceramax.api.repository.inventario.MovimientoInventarioProjection;
import com.ceramax.api.repository.inventario.ReservaInventarioProjection;
import com.ceramax.api.repository.inventario.TrasladoInventarioProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.NestedExceptionUtils;

@Service
@Transactional
public class InventarioService {

    private final InventarioConsultaRepository consultaRepository;
    private final InventarioOperacionRepository operacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StockBajoEventPublisher stockBajoEventPublisher;

    public InventarioService(
        InventarioConsultaRepository consultaRepository,
        InventarioOperacionRepository operacionRepository,
        UsuarioRepository usuarioRepository,
        ApplicationEventPublisher eventPublisher,
        StockBajoEventPublisher stockBajoEventPublisher
    ) {
        this.consultaRepository = consultaRepository;
        this.operacionRepository = operacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
        this.stockBajoEventPublisher = stockBajoEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<InventarioResponse> listarInventario() {
        return consultaRepository.listarInventario().stream().map(this::toInventario).toList();
    }

    @Transactional(readOnly = true)
    public List<StockDisponibleResponse> listarStockDisponible() {
        return consultaRepository.listarStockDisponible().stream().map(this::toStockDisponible).toList();
    }

    @Transactional(readOnly = true)
    public Integer consultarStockDisponible(UUID almacenId, UUID productoId) {
        Integer disponible = operacionRepository.consultarStockDisponible(almacenId, productoId);
        if (disponible == null) {
            throw new ResourceNotFoundException("No existe inventario del producto en el almacén indicado");
        }
        return disponible;
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarMovimientos() {
        return consultaRepository.listarMovimientos().stream().map(this::toMovimiento).toList();
    }

    @Transactional(readOnly = true)
    public List<TrasladoInventarioResponse> listarTrasladosEnTransito() {
        return consultaRepository.listarTrasladosEnTransito().stream().map(this::toTraslado).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaInventarioResponse> listarReservasActivas() {
        return consultaRepository.listarReservasActivas().stream().map(this::toReserva).toList();
    }

    @Transactional(readOnly = true)
    public List<AjusteInventarioResponse> listarAjustes() {
        return consultaRepository.listarAjustes().stream().map(this::toAjuste).toList();
    }

    public OperacionInventarioResponse registrarEntrada(EntradaInventarioRequest request) {
        UUID actorId = usuarioAutenticado().getId();
        String rol = usuarioAutenticado().getRol() != null ? usuarioAutenticado().getRol().getCodigo() : "";
        InventarioOperacionRepository.ProductoIngreso producto =
            operacionRepository.obtenerProductoIngreso(request.productoId());
        BigDecimal valorIngreso = producto.precioFinal().multiply(BigDecimal.valueOf(request.cantidad()));

        if ("LOGISTICA".equals(rol) && valorIngreso.compareTo(BigDecimal.valueOf(2000)) >= 0) {
            UUID solicitudId = operacionRepository.crearSolicitudEntrada(
                request.almacenId(),
                request.productoId(),
                request.cantidad(),
                request.observacion(),
                actorId
            );
            return new OperacionInventarioResponse(true);
        }

        StockBajoEventPublisher.StockSnapshot stockBajoAntes =
            stockBajoEventPublisher.capturarUbicacion(request.almacenId(), request.productoId());
        OperacionInventarioResponse response = ejecutarBooleano(() ->
            operacionRepository.registrarEntrada(
                request.almacenId(),
                request.productoId(),
                request.cantidad(),
                actorId,
                request.observacion()
            )
        );
        stockBajoEventPublisher.notificarCruces(
            stockBajoAntes,
            List.of(new StockBajoEventPublisher.InventarioUbicacion(request.almacenId(), request.productoId())),
            actorId
        );
        return response;
    }

    public List<SolicitudEntradaResponse> listarSolicitudesEntrada() {
        String rol = usuarioAutenticado().getRol() != null ? usuarioAutenticado().getRol().getCodigo() : "";
        if ("LOGISTICA".equals(rol)) {
            return operacionRepository.listarSolicitudesEntrada(usuarioAutenticado().getId());
        }
        if (!List.of("ADMIN", "JEFE_LOGISTICA").contains(rol)) {
            throw new ForbiddenException("No tienes permiso para ver solicitudes de entrada");
        }
        return operacionRepository.listarSolicitudesEntradaPendientes();
    }

    public OperacionInventarioResponse aprobarSolicitudEntrada(UUID solicitudId) {
        String rol = usuarioAutenticado().getRol() != null ? usuarioAutenticado().getRol().getCodigo() : "";
        if (!List.of("ADMIN", "JEFE_LOGISTICA").contains(rol)) {
            throw new ForbiddenException("Solo JEFE_LOGISTICA o ADMIN pueden aprobar entradas");
        }
        InventarioOperacionRepository.SolicitudEntradaDatos solicitud =
            operacionRepository.obtenerSolicitudEntrada(solicitudId);
        UUID actorId = usuarioAutenticado().getId();
        if (!operacionRepository.actualizarEstadoSolicitudEntrada(solicitudId, "APROBADO", actorId, "Entrada aprobada")) {
            throw new BusinessException("La solicitud ya no está pendiente");
        }
        OperacionInventarioResponse response = ejecutarBooleano(() ->
            operacionRepository.registrarEntrada(
                solicitud.almacenId(),
                solicitud.productoId(),
                solicitud.cantidad(),
                actorId,
                solicitud.observacion()
            )
        );
        return response;
    }

    public OperacionInventarioResponse rechazarSolicitudEntrada(UUID solicitudId, String motivo) {
        String rol = usuarioAutenticado().getRol() != null ? usuarioAutenticado().getRol().getCodigo() : "";
        if (!List.of("ADMIN", "JEFE_LOGISTICA").contains(rol)) {
            throw new ForbiddenException("Solo JEFE_LOGISTICA o ADMIN pueden rechazar entradas");
        }
        UUID actorId = usuarioAutenticado().getId();
        if (!operacionRepository.actualizarEstadoSolicitudEntrada(solicitudId, "RECHAZADO", actorId, motivo)) {
            throw new BusinessException("La solicitud ya no está pendiente");
        }
        return new OperacionInventarioResponse(true);
    }

    public AjusteCreadoResponse registrarAjuste(AjusteInventarioRequest request) {
        UUID actorId = usuarioAutenticado().getId();
        StockBajoEventPublisher.StockSnapshot stockBajoAntes =
            stockBajoEventPublisher.capturarUbicacion(request.almacenId(), request.productoId());
        UUID id = ejecutar(() ->
            operacionRepository.registrarAjuste(
                request.almacenId(),
                request.productoId(),
                request.nuevoStock(),
                request.motivo(),
                request.observacion(),
                actorId
            )
        );
        if (id == null) {
            throw new BusinessException("La base de datos no devolvió el identificador del ajuste");
        }
        stockBajoEventPublisher.notificarCruces(
            stockBajoAntes,
            List.of(new StockBajoEventPublisher.InventarioUbicacion(request.almacenId(), request.productoId())),
            actorId
        );
        return new AjusteCreadoResponse(id);
    }

    public TrasladoCreadoResponse iniciarTraslado(TrasladoInventarioRequest request) {
        UUID actorId = usuarioAutenticado().getId();
        StockBajoEventPublisher.StockSnapshot stockBajoAntes =
            stockBajoEventPublisher.capturarUbicacion(request.almacenOrigenId(), request.productoId());
        UUID id = ejecutar(() ->
            operacionRepository.iniciarTraslado(
                request.almacenOrigenId(),
                request.almacenDestinoId(),
                request.productoId(),
                request.cantidad(),
                actorId,
                request.tipoTraslado(),
                request.pedidoId(),
                request.detallePedidoId(),
                request.observacion()
            )
        );
        if (id == null) {
            throw new BusinessException("La base de datos no devolvió el identificador del traslado");
        }
        stockBajoEventPublisher.notificarCruces(
            stockBajoAntes,
            List.of(new StockBajoEventPublisher.InventarioUbicacion(
                request.almacenOrigenId(),
                request.productoId()
            )),
            actorId
        );
        return new TrasladoCreadoResponse(id);
    }

    public OperacionInventarioResponse confirmarRecepcion(UUID trasladoId, String observacion) {
        UUID actorId = usuarioAutenticado().getId();
        InventarioOperacionRepository.TrasladoDatos traslado = obtenerTraslado(trasladoId);
        StockBajoEventPublisher.StockSnapshot stockBajoAntes =
            stockBajoEventPublisher.capturarUbicacion(traslado.almacenDestinoId(), traslado.productoId());
        OperacionInventarioResponse response = ejecutarBooleano(() ->
            operacionRepository.confirmarRecepcion(trasladoId, actorId, observacion)
        );
        eventPublisher.publishEvent(new TrasladoRecibidoEvent(
            traslado.id(),
            traslado.codigo(),
            traslado.almacenDestinoId(),
            traslado.almacenDestinoNombre(),
            traslado.productoId(),
            traslado.productoCodigo(),
            traslado.productoNombre(),
            traslado.cantidad(),
            actorId
        ));
        stockBajoEventPublisher.notificarCruces(
            stockBajoAntes,
            List.of(new StockBajoEventPublisher.InventarioUbicacion(
                traslado.almacenDestinoId(),
                traslado.productoId()
            )),
            actorId
        );
        return response;
    }

    public OperacionInventarioResponse cancelarTraslado(UUID trasladoId, String motivo) {
        UUID actorId = usuarioAutenticado().getId();
        InventarioOperacionRepository.TrasladoDatos traslado = obtenerTraslado(trasladoId);
        StockBajoEventPublisher.StockSnapshot stockBajoAntes =
            stockBajoEventPublisher.capturarUbicacion(traslado.almacenOrigenId(), traslado.productoId());
        OperacionInventarioResponse response = ejecutarBooleano(() ->
            operacionRepository.cancelarTraslado(trasladoId, actorId, motivo)
        );
        stockBajoEventPublisher.notificarCruces(
            stockBajoAntes,
            List.of(new StockBajoEventPublisher.InventarioUbicacion(
                traslado.almacenOrigenId(),
                traslado.productoId()
            )),
            actorId
        );
        return response;
    }

    private InventarioOperacionRepository.TrasladoDatos obtenerTraslado(UUID trasladoId) {
        return operacionRepository.buscarTraslado(trasladoId).stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Traslado no encontrado"));
    }

    private Usuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("No se pudo resolver el usuario autenticado");
        }
        return usuarioRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("El usuario autenticado no existe como personal interno"));
    }

    private <T> T ejecutar(Operacion<T> operacion) {
        try {
            return operacion.ejecutar();
        } catch (DataAccessException exception) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
            String message = cause.getMessage();
            throw new BusinessException(
                message == null ? "La operación de inventario fue rechazada por la base de datos" : message
            );
        }
    }

    private OperacionInventarioResponse ejecutarBooleano(Operacion<Boolean> operacion) {
        boolean completada = ejecutar(operacion);
        if (!completada) {
            throw new BusinessException("La operación de inventario no pudo completarse");
        }
        return new OperacionInventarioResponse(true);
    }

    private InventarioResponse toInventario(InventarioProjection row) {
        return new InventarioResponse(
            row.getIdSucursal(),
            row.getSucursalNombre(),
            row.getIdAlmacen(),
            row.getAlmacenCodigo(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getCategoriaNombre(),
            row.getStockFisico(),
            row.getStockReservado(),
            row.getStockDisponible(),
            row.getStockMinimo(),
            row.getStockBajo()
        );
    }

    private StockDisponibleResponse toStockDisponible(InventarioProjection row) {
        return new StockDisponibleResponse(
            row.getIdSucursal(),
            row.getSucursalNombre(),
            row.getIdAlmacen(),
            row.getAlmacenCodigo(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getCategoriaNombre(),
            row.getStockFisico(),
            row.getStockReservado(),
            row.getStockDisponible()
        );
    }

    private MovimientoInventarioResponse toMovimiento(MovimientoInventarioProjection row) {
        return new MovimientoInventarioResponse(
            row.getId(),
            row.getCodigo(),
            toOffsetDateTime(row.getCreatedAt()),
            row.getIdAlmacen(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getIdUsuario(),
            row.getUsuarioNombre(),
            row.getIdPedido(),
            row.getIdTraslado(),
            row.getTipo(),
            row.getMotivo(),
            row.getCantidad(),
            row.getStockResultante(),
            row.getObservacion()
        );
    }

    private TrasladoInventarioResponse toTraslado(TrasladoInventarioProjection row) {
        return new TrasladoInventarioResponse(
            row.getId(),
            row.getCodigo(),
            row.getTipoTraslado(),
            row.getEstado(),
            row.getIdAlmacenOrigen(),
            row.getAlmacenOrigenNombre(),
            row.getIdAlmacenDestino(),
            row.getAlmacenDestinoNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getCantidad(),
            row.getIdPedido(),
            row.getPedidoCodigo(),
            toOffsetDateTime(row.getFechaInicio()),
            row.getDiasTransito()
        );
    }

    private AjusteInventarioResponse toAjuste(AjusteInventarioProjection row) {
        return new AjusteInventarioResponse(
            row.getId(),
            row.getCodigo(),
            row.getIdUsuarioEjecutor(),
            row.getUsuarioEjecutor(),
            row.getIdAlmacen(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getStockAnterior(),
            row.getCantidadAjustada(),
            row.getStockNuevo(),
            row.getMotivo(),
            row.getObservacion(),
            toOffsetDateTime(row.getCreatedAt())
        );
    }

    private ReservaInventarioResponse toReserva(ReservaInventarioProjection row) {
        return new ReservaInventarioResponse(
            row.getIdReserva(),
            row.getIdPedido(),
            row.getPedidoCodigo(),
            row.getIdDetallePedido(),
            row.getIdAlmacen(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getCantidad(),
            row.getEstado(),
            row.getTipoEntrega(),
            row.getClienteNombre(),
            toOffsetDateTime(row.getFechaPedido()),
            toOffsetDateTime(row.getFechaReserva())
        );
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws DataAccessException;
    }
}
