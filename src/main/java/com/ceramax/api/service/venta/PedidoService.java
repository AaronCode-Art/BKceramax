package com.ceramax.api.service.venta;

import com.ceramax.api.dto.request.AsignarDeliveryRequest;
import com.ceramax.api.dto.request.CambiarEstadoPedidoRequest;
import com.ceramax.api.dto.request.CotizarCheckoutWebRequest;
import com.ceramax.api.dto.request.CrearPedidoWebRequest;
import com.ceramax.api.dto.request.DetallePedidoRequest;
import com.ceramax.api.dto.request.VentaPresencialRequest;
import com.ceramax.api.dto.response.ComprobanteDatos;
import com.ceramax.api.dto.response.CheckoutCotizacionResponse;
import com.ceramax.api.dto.response.DatosEntregaResponse;
import com.ceramax.api.dto.response.DeliveryOpcionResponse;
import com.ceramax.api.dto.response.DetallePedidoResponse;
import com.ceramax.api.dto.response.PagoResponse;
import com.ceramax.api.dto.response.PedidoComprobanteResponse;
import com.ceramax.api.dto.response.PedidoEstadoHistorialResponse;
import com.ceramax.api.dto.response.PedidoPaginaResponse;
import com.ceramax.api.dto.response.PedidoResponse;
import com.ceramax.api.dto.response.PedidoResumenResponse;
import com.ceramax.api.dto.response.VentaPresencialResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ForbiddenException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.factory.ComprobanteFactory;
import com.ceramax.api.factory.PedidoFactory;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Sucursal;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.model.catalogo.Producto;
import com.ceramax.api.model.venta.DetallePedido;
import com.ceramax.api.model.venta.EstadoEnvio;
import com.ceramax.api.model.venta.Pago;
import com.ceramax.api.model.venta.Pedido;
import com.ceramax.api.observer.StockBajoEventPublisher;
import com.ceramax.api.observer.event.PedidoCreadoEvent;
import com.ceramax.api.observer.event.PedidoEstadoCambiadoEvent;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.SucursalRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.catalogo.GaleriaImagenRepository;
import com.ceramax.api.repository.catalogo.ProductoImagenProjection;
import com.ceramax.api.repository.catalogo.ProductoRepository;
import com.ceramax.api.repository.venta.DetallePedidoRepository;
import com.ceramax.api.repository.venta.EstadoEnvioRepository;
import com.ceramax.api.repository.venta.PagoRepository;
import com.ceramax.api.repository.venta.PedidoRepository;
import com.ceramax.api.repository.venta.PedidoEstadoHistorialProjection;
import com.ceramax.api.repository.venta.VentaOperacionRepository;
import com.ceramax.api.repository.venta.VentaOperacionRepository.ItemVenta;
import com.ceramax.api.repository.venta.VentaOperacionRepository.VentaPresencialResultado;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PagoRepository pagoRepository;
    private final EstadoEnvioRepository estadoEnvioRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;
    private final GaleriaImagenRepository galeriaImagenRepository;
    private final CarritoService carritoService;
    private final VentaOperacionRepository ventaOperacionRepository;
    private final PedidoFactory pedidoFactory;
    private final ComprobanteFactory comprobanteFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final StockBajoEventPublisher stockBajoEventPublisher;

    public PedidoService(
        PedidoRepository pedidoRepository,
        DetallePedidoRepository detallePedidoRepository,
        PagoRepository pagoRepository,
        EstadoEnvioRepository estadoEnvioRepository,
        ClienteRepository clienteRepository,
        UsuarioRepository usuarioRepository,
        SucursalRepository sucursalRepository,
        ProductoRepository productoRepository,
        GaleriaImagenRepository galeriaImagenRepository,
        CarritoService carritoService,
        VentaOperacionRepository ventaOperacionRepository,
        PedidoFactory pedidoFactory,
        ComprobanteFactory comprobanteFactory,
        ApplicationEventPublisher eventPublisher,
        EntityManager entityManager,
        StockBajoEventPublisher stockBajoEventPublisher
    ) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.pagoRepository = pagoRepository;
        this.estadoEnvioRepository = estadoEnvioRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.sucursalRepository = sucursalRepository;
        this.productoRepository = productoRepository;
        this.galeriaImagenRepository = galeriaImagenRepository;
        this.carritoService = carritoService;
        this.ventaOperacionRepository = ventaOperacionRepository;
        this.pedidoFactory = pedidoFactory;
        this.comprobanteFactory = comprobanteFactory;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.stockBajoEventPublisher = stockBajoEventPublisher;
    }

    public PedidoWebCheckout prepararCheckoutWeb(CrearPedidoWebRequest request) {
        if (!"SIMULADO".equals(request.metodoPago())
            || request.medioPagoSimulado() == null
            || !request.medioPagoSimulado().matches("YAPE|PLIN|TARJETA")) {
            throw new BusinessException("Selecciona Yape, Plin o tarjeta para el pago simulado");
        }
        Cliente cliente = clienteAutenticado();
        List<DetallePedidoRequest> detallesCarrito = carritoService.obtener()
            .stream()
            .map(item -> new DetallePedidoRequest(item.product().id(), item.quantity()))
            .toList();
        if (detallesCarrito.isEmpty()) {
            throw new BusinessException("El carrito está vacío");
        }
        CrearPedidoWebRequest requestCarrito = conDetalles(request, detallesCarrito);
        PedidoWebPreparado preparado = prepararPedidoWeb(requestCarrito, cliente);
        Pedido pedido = preparado.pedido();
        return new PedidoWebCheckout(
            cliente.getId(),
            cliente.getEmail(),
            pedido.getSubtotal(),
            pedido.getIgv(),
            pedido.getCostoEnvio(),
            pedido.getTotal(),
            requestCarrito
        );
    }

    @Transactional(readOnly = true)
    public CheckoutCotizacionResponse cotizarCheckoutWeb(CotizarCheckoutWebRequest request) {
        clienteAutenticado();
        List<DetallePedidoRequest> detallesCarrito = carritoService.obtener()
            .stream()
            .map(item -> new DetallePedidoRequest(item.product().id(), item.quantity()))
            .toList();
        if (detallesCarrito.isEmpty()) {
            throw new BusinessException("El carrito está vacío");
        }
        Map<UUID, Producto> productos = productosActivos(detallesCarrito);
        BigDecimal subtotal = detallesCarrito.stream()
            .map(linea -> productos.get(linea.productoId()).getPrecioFinal()
                .multiply(BigDecimal.valueOf(linea.cantidad())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        return calcularCotizacion(request.tipoEntrega(), subtotal);
    }

    public PedidoResponse confirmarCheckoutWebSimulado(CrearPedidoWebRequest request) {
        PedidoWebCheckout checkout = prepararCheckoutWeb(request);
        String transaccionId = "SIM-" + UUID.randomUUID();
        UUID pedidoId = registrarPedidoWebPagado(
            checkout.clienteId(),
            checkout.request(),
            checkout.total(),
            transaccionId,
            "SIMULADO",
            OffsetDateTime.now(),
            checkout.request().medioPagoSimulado(),
            "SIMULADO"
        );
        prepararPedidoWebPagado(pedidoId, transaccionId);
        return obtener(pedidoId);
    }

    private UUID registrarPedidoWebPagado(
        UUID clienteId,
        CrearPedidoWebRequest request,
        BigDecimal montoPagado,
        String transaccionId,
        String referencia,
        OffsetDateTime fechaPago,
        String metodoPago,
        String proveedor
    ) {
        Pago pagoExistente = pagoRepository.findByTransaccionId(transaccionId).orElse(null);
        if (pagoExistente != null) {
            if (!"APROBADO".equals(pagoExistente.getEstado())) {
                throw new BusinessException("La transacción ya está asociada a un pago no aprobado");
            }
            return pagoExistente.getPedido().getId();
        }
        Cliente cliente = clienteRepository.findById(clienteId)
            .filter(Cliente::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente del pago no encontrado"));
        PedidoWebPreparado preparado = prepararPedidoWeb(request, cliente);
        Pedido pedido = preparado.pedido();
        if (montoPagado.compareTo(pedido.getTotal()) != 0) {
            throw new BusinessException("El monto confirmado no coincide con el total vigente del carrito");
        }
        pedidoRepository.saveAndFlush(pedido);
        for (DetallePedido detalle : preparado.detalles()) {
            detalle.setPedido(pedido);
        }
        detallePedidoRepository.saveAllAndFlush(preparado.detalles());

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodoPago(metodoPago);
        pago.setEstado("APROBADO");
        pago.setMonto(montoPagado);
        pago.setProveedor(proveedor);
        pago.setReferencia(referencia);
        pago.setTransaccionId(transaccionId);
        pago.setFechaPago(fechaPago);
        pagoRepository.saveAndFlush(pago);
        entityManager.refresh(pedido);
        eventPublisher.publishEvent(new PedidoCreadoEvent(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getCanal(),
            pedido.getEstado().getCodigo(),
            pedido.getEstado().getNombre(),
            pedido.getClienteEmail(),
            null
        ));
        return pedido.getId();
    }

    private void prepararPedidoWebPagado(UUID pedidoId, String transaccionId) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
            .orElseThrow(() -> new BusinessException("El pedido asociado al pago simulado no existe"));
        pagoRepository.findByTransaccionId(transaccionId)
            .filter(pago -> pago.getPedido().getId().equals(pedidoId))
            .filter(pago -> "APROBADO".equals(pago.getEstado()))
            .filter(pago -> "SIMULADO".equals(pago.getProveedor()))
            .orElseThrow(() -> new BusinessException("No se encontró el pago simulado aprobado"));
        if ("PREPARACION".equals(pedido.getEstado().getCodigo()) && Boolean.TRUE.equals(pedido.getTieneReserva())) {
            return;
        }
        if (!"PENDIENTE_PAGO".equals(pedido.getEstado().getCodigo())) {
            throw new BusinessException("El pedido simulado ya no está pendiente de pago");
        }
        StockBajoEventPublisher.StockSnapshot stockBajoAntes =
            stockBajoEventPublisher.capturarPedido(pedidoId);
        if (!ventaOperacionRepository.reservarStock(pedidoId)) {
            throw new BusinessException("No hay inventario suficiente para completar la simulación");
        }
        stockBajoEventPublisher.notificarCrucesReservaPedido(stockBajoAntes, pedidoId, null);
        entityManager.refresh(pedido);
        EstadoEnvio preparacion = estadoEnvioRepository.findByCodigo("PREPARACION")
            .orElseThrow(() -> new IllegalStateException("No existe el estado PREPARACION"));
        if (!estadoEnvioRepository.existeTransicion(
            pedido.getEstado().getId(),
            preparacion.getId(),
            pedido.getTipoEntrega()
        )) {
            throw new IllegalStateException("No existe transición de pedido a PREPARACION");
        }
        pedido.setEstado(preparacion);
        pedidoRepository.saveAndFlush(pedido);
        vaciarCarritoCliente(pedidoId);
        entityManager.refresh(pedido);
        eventPublisher.publishEvent(new PedidoEstadoCambiadoEvent(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getEstado().getCodigo(),
            pedido.getEstado().getNombre(),
            pedido.getClienteEmail(),
            null,
            "PENDIENTE_PAGO",
            null,
            "APROBAR_PAGO_SIMULADO"
        ));
    }

    private void vaciarCarritoCliente(UUID pedidoId) {
        entityManager.createNativeQuery("""
            DELETE FROM carritodetalle cd
            USING carrito ca
            WHERE cd.id_carrito = ca.id
              AND ca.id_cliente = (SELECT id_cliente FROM pedido WHERE id = :pedidoId)
            """)
            .setParameter("pedidoId", pedidoId)
            .executeUpdate();
        entityManager.createNativeQuery("""
            UPDATE carrito
            SET estado = 'COMPRADO', updated_at = now()
            WHERE id_cliente = (SELECT id_cliente FROM pedido WHERE id = :pedidoId)
            """)
            .setParameter("pedidoId", pedidoId)
            .executeUpdate();
    }

    private PedidoWebPreparado prepararPedidoWeb(CrearPedidoWebRequest request, Cliente cliente) {
        UUID sucursalId = ejecutar(ventaOperacionRepository::sucursalActivaId);
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal activa no encontrada"));
        EstadoEnvio estado = estado("PENDIENTE_PAGO");
        List<DetallePedido> detalles = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<UUID, Producto> productos = productosActivos(request.detalles());
        List<UUID> productoIds = request.detalles().stream()
            .map(DetallePedidoRequest::productoId)
            .distinct()
            .toList();
        Map<UUID, String> imagenes = imagenesPrincipales(productoIds);

        for (DetallePedidoRequest linea : request.detalles()) {
            Producto producto = productos.get(linea.productoId());
            BigDecimal precio = producto.getPrecioFinal();
            BigDecimal importe = precio.multiply(BigDecimal.valueOf(linea.cantidad())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(importe);
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setProductoCodigo(producto.getCodigo());
            detalle.setProductoNombre(producto.getNombre());
            detalle.setCategoriaNombre(producto.getCategoria().getNombre());
            detalle.setProductoImagenUrl(imagenes.get(producto.getId()));
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(precio);
            detalle.setDescuentoPorcentaje(producto.getDescuentoPorcentaje());
            detalle.setSubtotal(importe);
            detalle.setEstadoStock("PENDIENTE");
            detalles.add(detalle);
        }

        CheckoutCotizacionResponse cotizacion = calcularCotizacion(request.tipoEntrega(), subtotal);
        Pedido pedido = pedidoFactory.crearPedidoWeb(
            request,
            cliente,
            sucursal,
            estado,
            cotizacion.subtotal(),
            cotizacion.costoEnvio(),
            cotizacion.igv(),
            cotizacion.total()
        );
        return new PedidoWebPreparado(pedido, detalles);
    }

    private Map<UUID, Producto> productosActivos(List<DetallePedidoRequest> detalles) {
        List<UUID> productoIds = detalles.stream()
            .map(DetallePedidoRequest::productoId)
            .distinct()
            .toList();
        Map<UUID, Producto> productos = productoRepository.findByIdIn(productoIds)
            .stream()
            .collect(Collectors.toMap(Producto::getId, Function.identity()));
        for (DetallePedidoRequest linea : detalles) {
            Producto producto = productos.get(linea.productoId());
            if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
                throw new ResourceNotFoundException("Producto activo no encontrado");
            }
            if (!Boolean.TRUE.equals(producto.getCategoria().getActivo())) {
                throw new ResourceNotFoundException("La categoría del producto no está activa");
            }
            if (producto.getPrecioFinal() == null) {
                throw new BusinessException("El producto no tiene precio final calculado por la base de datos");
            }
        }
        return productos;
    }

    private CheckoutCotizacionResponse calcularCotizacion(String tipoEntrega, BigDecimal subtotal) {
        BigDecimal costoEnvio = "DELIVERY".equals(tipoEntrega)
            ? ejecutar(() -> ventaOperacionRepository.parametroNumerico("COSTO_DELIVERY"))
            : BigDecimal.ZERO;
        BigDecimal porcentajeIgv = ejecutar(() -> ventaOperacionRepository.parametroNumerico("IGV_PORCENTAJE"));
        BigDecimal igv = subtotal.multiply(porcentajeIgv).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv).add(costoEnvio).setScale(2, RoundingMode.HALF_UP);
        return new CheckoutCotizacionResponse(subtotal, igv, costoEnvio, total);
    }

    private Map<UUID, String> imagenesPrincipales(List<UUID> productoIds) {
        if (productoIds.isEmpty()) {
            return Map.of();
        }
        return galeriaImagenRepository.listarImagenPrincipalPorProductos(productoIds)
            .stream()
            .collect(Collectors.toMap(
                ProductoImagenProjection::getProductoId,
                ProductoImagenProjection::getUrl,
                (primera, ignorada) -> primera
            ));
    }

    private CrearPedidoWebRequest conDetalles(
        CrearPedidoWebRequest request,
        List<DetallePedidoRequest> detalles
    ) {
        return new CrearPedidoWebRequest(
            request.tipoEntrega(),
            request.tipoComprobante(),
            request.ruc(),
            request.razonSocial(),
            request.envioDepartamento(),
            request.envioProvincia(),
            request.envioDistrito(),
            request.envioDireccion(),
            request.envioCodigoPostal(),
            request.envioReferencia(),
            request.envioIdUbigeo(),
            request.metodoPago(),
            request.medioPagoSimulado(),
            detalles
        );
    }

    public record PedidoWebCheckout(
        UUID clienteId,
        String clienteEmail,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal costoEnvio,
        BigDecimal total,
        CrearPedidoWebRequest request
    ) {}

    private record PedidoWebPreparado(Pedido pedido, List<DetallePedido> detalles) {}

    public VentaPresencialResponse registrarVentaPresencial(VentaPresencialRequest request) {
        Usuario vendedor = usuarioInternoAutenticado();
        String rol = rolActual();
        if (!"ADMIN".equals(rol) && !"VENDEDOR".equals(rol)) {
            throw new ForbiddenException("Solo ADMIN o VENDEDOR pueden registrar ventas presenciales");
        }
        ComprobanteDatos comprobante = pedidoFactory.validarPedidoPresencial(
            request.tipoComprobante(),
            request.ruc(),
            request.razonSocial()
        );
        Cliente cliente = request.clienteId() == null
            ? null
            : clienteRepository.findById(request.clienteId())
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente activo no encontrado"));
        List<ItemVenta> items = request.detalles().stream()
            .map(linea -> new ItemVenta(linea.productoId(), linea.cantidad()))
            .toList();
        StockBajoEventPublisher.StockSnapshot stockBajoAntes =
            stockBajoEventPublisher.capturarProductos(items.stream().map(ItemVenta::id_producto).toList());
        VentaPresencialResultado resultado = ejecutar(() ->
            ventaOperacionRepository.registrarVentaPresencial(
                vendedor.getId(),
                items,
                cliente == null ? null : cliente.getId(),
                comprobante.tipo(),
                comprobanteFactory.normalizarMetodoPago(request.metodoPago()),
                comprobante.ruc(),
                comprobante.razonSocial()
            )
        );
        stockBajoEventPublisher.notificarCrucesVentaPresencial(
            stockBajoAntes,
            resultado.pedidoId(),
            vendedor.getId()
        );
        Pedido pedido = pedidoRepository.findById(resultado.pedidoId())
            .orElseThrow(() -> new ResourceNotFoundException("La venta se registró pero el pedido no está disponible"));
        eventPublisher.publishEvent(new PedidoCreadoEvent(
            pedido.getId(),
            resultado.codigoPedido(),
            pedido.getCanal(),
            pedido.getEstado().getCodigo(),
            pedido.getEstado().getNombre(),
            pedido.getClienteEmail(),
            vendedor.getId()
        ));
        return new VentaPresencialResponse(
            resultado.pedidoId(),
            resultado.codigoPedido(),
            resultado.tipoResolucion(),
            resultado.mensaje()
        );
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarMisPedidos() {
        Cliente cliente = clienteAutenticado();
        return toResponses(pedidoRepository.findByCliente_IdOrderByCreatedAtDesc(cliente.getId()));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidosVisibles() {
        String rol = rolActual();
        if ("CLIENTE".equals(rol)) {
            return listarMisPedidos();
        }
        if ("DELIVERY".equals(rol)) {
            Usuario delivery = usuarioInternoAutenticado();
            return toResponses(pedidoRepository.findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
                delivery.getId(),
                List.of("LISTO_DESPACHO", "EN_RUTA", "EN_CAMINO")
            ));
        }
        return toResponses(pedidoRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarHistorialDelivery() {
        String rol = rolActual();
        if (!List.of("ADMIN", "JEFE_LOGISTICA", "DELIVERY").contains(rol)) {
            throw new ForbiddenException("Solo ADMIN o DELIVERY pueden consultar el historial de pedidos");
        }
        if ("ADMIN".equals(rol)) {
            return toResponses(pedidoRepository.findAllByOrderByCreatedAtDesc());
        }
        Usuario delivery = usuarioInternoAutenticado();
        return toResponses(pedidoRepository.findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
            delivery.getId(),
            List.of("COMPLETADA", "NO_ENTREGADA")
        ));
    }

    @Transactional(readOnly = true)
    public List<PedidoEstadoHistorialResponse> listarHistorialEstados(UUID pedidoId) {
        Pedido pedido = pedido(pedidoId);
        String rol = rolActual();
        if (!List.of("ADMIN", "JEFE_LOGISTICA", "LOGISTICA", "DELIVERY").contains(rol)) {
            throw new ForbiddenException("No tienes permiso para consultar el historial del pedido");
        }
        if ("DELIVERY".equals(rol)) {
            validarAccesoLectura(pedido);
        }
        return pedidoRepository.findHistorialEstados(pedidoId).stream()
            .map(this::toHistorialEstadoResponse)
            .toList();
    }

    private PedidoEstadoHistorialResponse toHistorialEstadoResponse(PedidoEstadoHistorialProjection event) {
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

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarBandejaLogistica() {
        if (!List.of("ADMIN", "JEFE_LOGISTICA", "LOGISTICA").contains(rolActual())) {
            throw new ForbiddenException("Solo ADMIN o LOGISTICA pueden consultar la bandeja logística");
        }
        return toResponses(pedidoRepository.findByEstado_CodigoInOrderByCreatedAtDesc(List.of(
            "PREPARACION",
            "LISTO_DESPACHO",
            "LISTO_PARA_RECOGER",
            "EN_RUTA",
            "EN_CAMINO",
            "NO_ENTREGADA"
        )));
    }

    @Transactional(readOnly = true)
    public List<DeliveryOpcionResponse> listarDeliveriesActivos() {
        if (!List.of("ADMIN", "JEFE_LOGISTICA", "LOGISTICA").contains(rolActual())) {
            throw new ForbiddenException("Solo ADMIN o LOGISTICA pueden consultar deliveries");
        }
        return usuarioRepository.findByRol_CodigoAndActivoTrueOrderByApellidosAscNombresAsc("DELIVERY")
            .stream()
            .map(delivery -> new DeliveryOpcionResponse(
                delivery.getId(),
                delivery.getNombres(),
                delivery.getApellidos(),
                delivery.getTelefono()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarTransicionesDisponibles(UUID pedidoId) {
        Pedido pedido = pedido(pedidoId);
        validarAccesoLectura(pedido);
        String rol = rolActual();
        Usuario actor = usuarioInternoAutenticado();
        return estadoEnvioRepository.listarCodigosDestinoTransiciones(
                pedido.getEstado().getId(),
                pedido.getTipoEntrega()
            ).stream()
            .filter(destino -> puedeCambiarEstado(pedido, destino, rol, actor))
            .toList();
    }

    @Transactional(readOnly = true)
    public PedidoPaginaResponse listarPaginaVisible(Pageable pageable) {
        String rol = rolActual();
        Page<Pedido> pagina;
        if ("CLIENTE".equals(rol)) {
            Cliente cliente = clienteAutenticado();
            pagina = pedidoRepository.findByCliente_IdOrderByCreatedAtDesc(cliente.getId(), pageable);
        } else if ("DELIVERY".equals(rol)) {
            Usuario delivery = usuarioInternoAutenticado();
            pagina = pedidoRepository.findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
                delivery.getId(),
                List.of("LISTO_DESPACHO", "EN_RUTA", "EN_CAMINO"),
                pageable
            );
        } else {
            pagina = pedidoRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return new PedidoPaginaResponse(
            pagina.getContent().stream().map(this::toResumen).toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.isFirst(),
            pagina.isLast(),
            pagina.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(UUID pedidoId) {
        Pedido pedido = pedido(pedidoId);
        validarAccesoLectura(pedido);
        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoComprobanteResponse obtenerDatosComprobante(UUID pedidoId) {
        Pedido pedido = pedido(pedidoId);
        validarAccesoLectura(pedido);
        PedidoResponse detallePedido = toResponse(pedido);
        PagoResponse pagoAprobado = detallePedido.pagos().stream()
            .filter(pago -> "APROBADO".equals(pago.estado()))
            .reduce((primero, siguiente) -> siguiente)
            .orElseThrow(() -> new BusinessException("Solo se puede generar el comprobante de un pedido pagado"));
        return new PedidoComprobanteResponse(
            detallePedido,
            pedido.getClienteNombre(),
            pedido.getClienteTipoDocumento(),
            pedido.getClienteNumeroDocumento(),
            pedido.getClienteEmail(),
            pedido.getRuc(),
            pedido.getRazonSocial(),
            pedido.getSucursalNombre(),
            pedido.getSucursalDireccion(),
            pagoAprobado.fechaPago() == null ? pedido.getCreatedAt() : pagoAprobado.fechaPago()
        );
    }

    public PedidoResponse cambiarEstado(UUID pedidoId, CambiarEstadoPedidoRequest request) {
        Pedido pedido = pedido(pedidoId);
        Usuario actor = usuarioInternoAutenticado();
        String estadoAnteriorCodigo = pedido.getEstado().getCodigo();
        EstadoEnvio siguiente = estado(request.estadoCodigo());
        String rol = rolActual();

        if (!estadoEnvioRepository.existeTransicion(pedido.getEstado().getId(), siguiente.getId(), pedido.getTipoEntrega())) {
            throw new BusinessException("La transición de estado no está permitida para este tipo de entrega");
        }
        validarPermisoCambioEstado(pedido, siguiente.getCodigo(), rol, actor);

        if ("PREPARACION".equals(siguiente.getCodigo())) {
            StockBajoEventPublisher.StockSnapshot stockBajoAntes =
                stockBajoEventPublisher.capturarPedido(pedido.getId());
            boolean reservada = ejecutar(() -> ventaOperacionRepository.reservarStock(pedido.getId()));
            if (!reservada) {
                throw new BusinessException("No se pudo reservar el inventario para el pedido");
            }
            stockBajoEventPublisher.notificarCrucesReservaPedido(stockBajoAntes, pedido.getId(), actor.getId());
            entityManager.refresh(pedido);
        }
        if ("CANCELADO".equals(siguiente.getCodigo()) && Boolean.TRUE.equals(pedido.getTieneReserva())) {
            boolean liberada = ejecutar(() -> ventaOperacionRepository.liberarReserva(pedido.getId()));
            if (!liberada) {
                throw new BusinessException("No se pudo liberar la reserva del pedido");
            }
            entityManager.refresh(pedido);
        }
        boolean pedidoPreparadoParaEntrega =
            ("LISTO_DESPACHO".equals(siguiente.getCodigo()) && "DELIVERY".equals(pedido.getTipoEntrega()))
                || ("LISTO_PARA_RECOGER".equals(siguiente.getCodigo())
                    && "RECOJO_TIENDA".equals(pedido.getTipoEntrega()));
        boolean pedidoAnteriorPendienteDeDespacho =
            Boolean.TRUE.equals(pedido.getTieneReserva())
                && (("EN_RUTA".equals(siguiente.getCodigo()) && "DELIVERY".equals(pedido.getTipoEntrega()))
                    || ("COMPLETADA".equals(siguiente.getCodigo())
                        && "RECOJO_TIENDA".equals(pedido.getTipoEntrega())));
        if (pedidoPreparadoParaEntrega || pedidoAnteriorPendienteDeDespacho) {
            boolean despachado = ejecutar(() ->
                ventaOperacionRepository.despacharReservas(pedido.getId(), actor.getId())
            );
            if (!despachado) {
                throw new BusinessException("No se pudo despachar el pedido reservado");
            }
            entityManager.refresh(pedido);
        }

        pedido.setEstado(siguiente);
        pedidoRepository.saveAndFlush(pedido);
        entityManager.refresh(pedido);
        eventPublisher.publishEvent(new PedidoEstadoCambiadoEvent(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getEstado().getCodigo(),
            pedido.getEstado().getNombre(),
            pedido.getClienteEmail(),
            pedido.getDelivery() == null ? null : pedido.getDelivery().getEmail(),
            estadoAnteriorCodigo,
            actor.getId(),
            "CAMBIAR_ESTADO"
        ));
        return toResponse(pedido);
    }

    public PedidoResponse cancelarPedidoCliente(UUID pedidoId) {
        Cliente cliente = clienteAutenticado();
        Pedido pedido = pedido(pedidoId);
        if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(cliente.getId())) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        String estadoAnteriorCodigo = pedido.getEstado().getCodigo();
        EstadoEnvio cancelado = estado("CANCELADO");
        if (!estadoEnvioRepository.existeTransicion(pedido.getEstado().getId(), cancelado.getId(), pedido.getTipoEntrega())) {
            throw new BusinessException("El pedido ya no se puede cancelar desde su estado actual");
        }
        if (Boolean.TRUE.equals(pedido.getTieneReserva())) {
            boolean liberada = ejecutar(() -> ventaOperacionRepository.liberarReserva(pedido.getId()));
            if (!liberada) {
                throw new BusinessException("No se pudo liberar la reserva del pedido");
            }
            entityManager.refresh(pedido);
        }
        pedido.setEstado(cancelado);
        pedidoRepository.saveAndFlush(pedido);
        entityManager.refresh(pedido);
        eventPublisher.publishEvent(new PedidoEstadoCambiadoEvent(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getEstado().getCodigo(),
            pedido.getEstado().getNombre(),
            pedido.getClienteEmail(),
            null,
            estadoAnteriorCodigo,
            null,
            "CANCELAR_PEDIDO"
        ));
        return toResponse(pedido);
    }

    public PedidoResponse asignarDelivery(UUID pedidoId, AsignarDeliveryRequest request) {
        Pedido pedido = pedido(pedidoId);
        Usuario actor = usuarioInternoAutenticado();
        if (!"ADMIN".equals(rolActual()) && !"JEFE_LOGISTICA".equals(rolActual()) && !"LOGISTICA".equals(rolActual())) {
            throw new ForbiddenException("Solo ADMIN o LOGISTICA pueden asignar pedidos a delivery");
        }
        if (!"DELIVERY".equals(pedido.getTipoEntrega())) {
            throw new BusinessException("Solo los pedidos con entrega DELIVERY se pueden asignar");
        }
        if (!"LISTO_DESPACHO".equals(pedido.getEstado().getCodigo())) {
            throw new BusinessException("El pedido debe estar listo para despacho antes de asignar un delivery");
        }
        if (pedido.getDelivery() != null) {
            throw new BusinessException("El pedido ya tiene un delivery asignado");
        }
        Usuario delivery = usuarioRepository.findById(request.usuarioId())
            .filter(Usuario::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario delivery activo no encontrado"));
        if (delivery.getRol() == null || !"DELIVERY".equals(delivery.getRol().getCodigo())) {
            throw new BusinessException("El usuario asignado debe tener el rol DELIVERY");
        }
        if ("LOGISTICA".equals(rolActual()) || "JEFE_LOGISTICA".equals(rolActual())) {
            pedido.setLogistica(actor);
            pedido.setLogisticaNombre(actor.getNombres() + " " + actor.getApellidos());
        }
        pedido.setDelivery(delivery);
        pedido.setDeliveryNombre(delivery.getNombres() + " " + delivery.getApellidos());
        pedidoRepository.saveAndFlush(pedido);
        entityManager.refresh(pedido);
        eventPublisher.publishEvent(new PedidoEstadoCambiadoEvent(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getEstado().getCodigo(),
            pedido.getEstado().getNombre(),
            pedido.getClienteEmail(),
            delivery.getEmail(),
            pedido.getEstado().getCodigo(),
            actor.getId(),
            "ASIGNAR_DELIVERY"
        ));
        return toResponse(pedido);
    }

    private void validarPermisoCambioEstado(Pedido pedido, String destino, String rol, Usuario actor) {
        if (!puedeCambiarEstado(pedido, destino, rol, actor)) {
            throw new ForbiddenException("El rol actual no puede realizar este cambio de estado");
        }
    }

    private boolean puedeCambiarEstado(Pedido pedido, String destino, String rol, Usuario actor) {
        return switch (rol) {
            case "ADMIN" -> true;
            case "LOGISTICA", "JEFE_LOGISTICA" -> List.of(
                "PREPARACION",
                "LISTO_DESPACHO",
                "LISTO_PARA_RECOGER",
                "EN_RUTA",
                "CANCELADO"
            )
                .contains(destino);
            case "VENDEDOR" -> "CANCELADO".equals(destino) && "WEB".equals(pedido.getCanal());
            case "DELIVERY" -> "DELIVERY".equals(pedido.getTipoEntrega())
                && List.of("EN_RUTA", "EN_CAMINO", "COMPLETADA", "NO_ENTREGADA").contains(destino)
                && pedido.getDelivery() != null
                && pedido.getDelivery().getId().equals(actor.getId());
            default -> false;
        };
    }

    private void validarAccesoLectura(Pedido pedido) {
        String rol = rolActual();
        if ("CLIENTE".equals(rol)) {
            Cliente cliente = clienteAutenticado();
            if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(cliente.getId())) {
                throw new ResourceNotFoundException("Pedido no encontrado");
            }
        } else if ("DELIVERY".equals(rol)) {
            Usuario delivery = usuarioInternoAutenticado();
            if (pedido.getDelivery() == null || !pedido.getDelivery().getId().equals(delivery.getId())) {
                throw new ResourceNotFoundException("Pedido no encontrado");
            }
        }
    }

    private Pedido pedido(UUID pedidoId) {
        return pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    }

    private EstadoEnvio estado(String codigo) {
        return estadoEnvioRepository.findByCodigo(codigo)
            .orElseThrow(() -> new ResourceNotFoundException("Estado de pedido no encontrado: " + codigo));
    }

    private Cliente clienteAutenticado() {
        String email = autenticacion().getName();
        return clienteRepository.findByEmail(email)
            .filter(Cliente::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente activo autenticado no encontrado"));
    }

    private Usuario usuarioInternoAutenticado() {
        String email = autenticacion().getName();
        return usuarioRepository.findByEmail(email)
            .filter(Usuario::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario interno activo no encontrado"));
    }

    private Authentication autenticacion() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Se requiere autenticación");
        }
        return authentication;
    }

    private String rolActual() {
        return autenticacion().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("El usuario autenticado no tiene un rol"));
    }

    private <T> T ejecutar(Operacion<T> operacion) {
        try {
            return operacion.ejecutar();
        } catch (DataAccessException exception) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
            String message = cause.getMessage();
            throw new BusinessException(
                message == null ? "La operación de venta fue rechazada por la base de datos" : message
            );
        }
    }

    private PedidoResponse toResponse(Pedido pedido) {
        return toResponses(List.of(pedido)).get(0);
    }

    private List<PedidoResponse> toResponses(List<Pedido> pedidos) {
        if (pedidos.isEmpty()) {
            return List.of();
        }
        List<UUID> pedidoIds = pedidos.stream().map(Pedido::getId).toList();
        Map<UUID, List<DetallePedido>> detallesPorPedido =
            detallePedidoRepository.findByPedido_IdInOrderByPedido_IdAscIdAsc(pedidoIds)
                .stream()
                .collect(Collectors.groupingBy(detalle -> detalle.getPedido().getId()));
        Map<UUID, List<Pago>> pagosPorPedido =
            pagoRepository.findByPedido_IdInOrderByPedido_IdAscCreatedAtAsc(pedidoIds)
                .stream()
                .collect(Collectors.groupingBy(pago -> pago.getPedido().getId()));

        return pedidos.stream()
            .map(pedido -> toResponse(
                pedido,
                detallesPorPedido.getOrDefault(pedido.getId(), List.of()),
                pagosPorPedido.getOrDefault(pedido.getId(), List.of())
            ))
            .toList();
    }

    private PedidoResponse toResponse(Pedido pedido, List<DetallePedido> lineas, List<Pago> pagosDelPedido) {
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
            nombreEstadoVisible(pedido),
            pedido.getLogistica() == null ? null : pedido.getLogistica().getId(),
            pedido.getLogisticaNombre(),
            pedido.getDelivery() == null ? null : pedido.getDelivery().getId(),
            pedido.getDeliveryNombre(),
            pedido.getTipoComprobante(),
            pedido.getSerie(),
            pedido.getNumeroComprobante(),
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
            new DatosEntregaResponse(
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
            )
        );
    }

    private String nombreEstadoVisible(Pedido pedido) {
        if (!"CLIENTE".equals(rolActual())) {
            return pedido.getEstado().getNombre();
        }
        return switch (pedido.getEstado().getCodigo()) {
            case "PENDIENTE_PAGO", "PREPARACION" -> "Pendiente";
            case "LISTO_DESPACHO" -> "Preparado";
            case "LISTO_PARA_RECOGER" -> "Listo para recoger";
            case "EN_RUTA" -> "En ruta";
            case "EN_CAMINO" -> "En camino";
            case "COMPLETADA" -> "RECOJO_TIENDA".equals(pedido.getTipoEntrega())
                ? "Recogido en tienda"
                : "Entregado";
            case "NO_ENTREGADA" -> "No se pudo entregar";
            case "CANCELADO" -> "Cancelado";
            default -> pedido.getEstado().getNombre();
        };
    }

    private PedidoResumenResponse toResumen(Pedido pedido) {
        return new PedidoResumenResponse(
            pedido.getId(),
            pedido.getCodigo(),
            pedido.getCanal(),
            pedido.getTipoEntrega(),
            pedido.getEstado().getCodigo(),
            nombreEstadoVisible(pedido),
            pedido.getTipoComprobante(),
            pedido.getSerie(),
            pedido.getNumeroComprobante(),
            pedido.getTotal(),
            pedido.getCreatedAt(),
            new DatosEntregaResponse(
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
            )
        );
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws DataAccessException;
    }
}
