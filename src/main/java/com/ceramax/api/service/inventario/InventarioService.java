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
import com.ceramax.api.mappers.inventario.InventarioMapper;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.observer.StockBajoEventPublisher;
import com.ceramax.api.observer.event.TrasladoRecibidoEvent;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.inventario.InventarioConsultaRepository;
import com.ceramax.api.repository.inventario.InventarioOperacionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final InventarioMapper inventarioMapper;

    public InventarioService(
        InventarioConsultaRepository consultaRepository,
        InventarioOperacionRepository operacionRepository,
        UsuarioRepository usuarioRepository,
        ApplicationEventPublisher eventPublisher,
        StockBajoEventPublisher stockBajoEventPublisher,
        InventarioMapper inventarioMapper
    ) {
        this.consultaRepository = consultaRepository;
        this.operacionRepository = operacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
        this.stockBajoEventPublisher = stockBajoEventPublisher;
        this.inventarioMapper = inventarioMapper;
    }

    @Transactional(readOnly = true)
    public Page<InventarioResponse> listarInventario(Pageable pageable) {
        return consultaRepository.listarInventario(pageable).map(inventarioMapper::toInventario);
    }

    @Transactional(readOnly = true)
    public Page<StockDisponibleResponse> listarStockDisponible(Pageable pageable) {
        return consultaRepository.listarStockDisponible(pageable).map(inventarioMapper::toStockDisponible);
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
    public Page<MovimientoInventarioResponse> listarMovimientos(Pageable pageable) {
        return consultaRepository.listarMovimientos(pageable).map(inventarioMapper::toMovimiento);
    }

    @Transactional(readOnly = true)
    public Page<TrasladoInventarioResponse> listarTrasladosEnTransito(Pageable pageable) {
        return consultaRepository.listarTrasladosEnTransito(pageable).map(inventarioMapper::toTraslado);
    }

    @Transactional(readOnly = true)
    public Page<ReservaInventarioResponse> listarReservasActivas(Pageable pageable) {
        return consultaRepository.listarReservasActivas(pageable).map(inventarioMapper::toReserva);
    }

    @Transactional(readOnly = true)
    public Page<AjusteInventarioResponse> listarAjustes(Pageable pageable) {
        return consultaRepository.listarAjustes(pageable).map(inventarioMapper::toAjuste);
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

    public Page<SolicitudEntradaResponse> listarSolicitudesEntrada(Pageable pageable) {
        String rol = usuarioAutenticado().getRol() != null ? usuarioAutenticado().getRol().getCodigo() : "";
        if ("LOGISTICA".equals(rol)) {
            return operacionRepository.listarSolicitudesEntrada(usuarioAutenticado().getId(), pageable);
        }
        if (!List.of("ADMIN", "JEFE_LOGISTICA").contains(rol)) {
            throw new ForbiddenException("No tienes permiso para ver solicitudes de entrada");
        }
        return operacionRepository.listarSolicitudesEntradaPendientes(pageable);
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

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws DataAccessException;
    }
}
