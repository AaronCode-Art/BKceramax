package com.ceramax.api.service.venta;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.AsignarDeliveryRequest;
import com.ceramax.api.dto.request.CambiarEstadoPedidoRequest;
import com.ceramax.api.dto.response.PedidoResponse;
import com.ceramax.api.dto.response.DeliveryOpcionResponse;
import com.ceramax.api.repository.venta.PedidoEstadoHistorialProjection;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.factory.ComprobanteFactory;
import com.ceramax.api.factory.PedidoFactory;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Rol;
import com.ceramax.api.model.venta.EstadoEnvio;
import com.ceramax.api.model.venta.Pedido;
import com.ceramax.api.observer.StockBajoEventPublisher;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.SucursalRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.catalogo.GaleriaImagenRepository;
import com.ceramax.api.repository.catalogo.ProductoRepository;
import com.ceramax.api.repository.venta.DetallePedidoRepository;
import com.ceramax.api.repository.venta.EstadoEnvioRepository;
import com.ceramax.api.repository.venta.PagoRepository;
import com.ceramax.api.repository.venta.PedidoRepository;
import com.ceramax.api.repository.venta.VentaOperacionRepository;
import com.ceramax.api.factory.ComprobanteFactory;
import com.ceramax.api.factory.PedidoFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PedidoServiceDeliveryTest {

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final DetallePedidoRepository detallePedidoRepository = mock(DetallePedidoRepository.class);
    private final PagoRepository pagoRepository = mock(PagoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EstadoEnvioRepository estadoEnvioRepository = mock(EstadoEnvioRepository.class);
    private final VentaOperacionRepository ventaOperacionRepository = mock(VentaOperacionRepository.class);
    private final PedidoService pedidoService = new PedidoService(
        pedidoRepository,
        detallePedidoRepository,
        pagoRepository,
        estadoEnvioRepository,
        clienteRepository,
        usuarioRepository,
        mock(SucursalRepository.class),
        mock(ProductoRepository.class),
        mock(GaleriaImagenRepository.class),
        mock(CarritoService.class),
        ventaOperacionRepository,
        mock(PedidoFactory.class),
        mock(ComprobanteFactory.class),
        mock(ApplicationEventPublisher.class),
        mock(EntityManager.class),
        mock(StockBajoEventPublisher.class)
    );

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bandejaDeDeliveryIncluyePedidosListosEnRutaYEnCamino() {
        UUID deliveryId = UUID.randomUUID();
        Usuario delivery = new Usuario();
        delivery.setId(deliveryId);
        delivery.setActivo(true);
        autenticar("delivery@example.com", "DELIVERY");
        when(usuarioRepository.findByEmail("delivery@example.com")).thenReturn(Optional.of(delivery));
        when(pedidoRepository.findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
            deliveryId,
            List.of("LISTO_DESPACHO", "EN_RUTA", "EN_CAMINO")
        )).thenReturn(List.of());

        pedidoService.listarPedidosVisibles();

        verify(pedidoRepository).findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
            deliveryId,
            List.of("LISTO_DESPACHO", "EN_RUTA", "EN_CAMINO")
        );
    }

    @Test
    void historialDeliverySoloConsultaPedidosFinalizadosDelUsuarioAutenticado() {
        UUID deliveryId = UUID.randomUUID();
        Usuario delivery = new Usuario();
        delivery.setId(deliveryId);
        delivery.setActivo(true);
        autenticar("delivery@example.com", "DELIVERY");
        when(usuarioRepository.findByEmail("delivery@example.com")).thenReturn(Optional.of(delivery));
        when(pedidoRepository.findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
            deliveryId,
            List.of("COMPLETADA", "NO_ENTREGADA")
        )).thenReturn(List.of());

        List<PedidoResponse> history = pedidoService.listarHistorialDelivery();

        assertEquals(List.of(), history);
        verify(pedidoRepository).findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
            deliveryId,
            List.of("COMPLETADA", "NO_ENTREGADA")
        );
    }

    @Test
    void historialAdminIncluyePedidosDeTodosLosEstados() {
        List<Pedido> pedidos = List.of(
            pedido("PED-ENTREGADO"),
            pedido("PED-NO-ENTREGADO"),
            pedido("PED-PENDIENTE")
        );
        pedidos.get(2).getEstado().setCodigo("PREPARACION");
        autenticar("admin@example.com", "ADMIN");
        when(pedidoRepository.findAllByOrderByCreatedAtDesc()).thenReturn(pedidos);

        List<PedidoResponse> history = pedidoService.listarHistorialDelivery();

        assertEquals(3, history.size());
        assertEquals("PREPARACION", history.get(2).estadoCodigo());
        verify(pedidoRepository).findAllByOrderByCreatedAtDesc();
        verify(pedidoRepository, never()).findByEstado_CodigoInOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.anyList()
        );
        verify(pedidoRepository, never()).findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void logisticaPuedeConsultarElHistorialDeEstadosConFechaYActor() {
        UUID pedidoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        EstadoEnvio actual = new EstadoEnvio();
        actual.setId(UUID.randomUUID());
        actual.setCodigo("EN_CAMINO");
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstado(actual);
        PedidoEstadoHistorialProjection change = mock(PedidoEstadoHistorialProjection.class);
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.findHistorialEstados(pedidoId)).thenReturn(List.of(change));
        when(change.getId()).thenReturn(UUID.randomUUID());
        when(change.getEstadoAnteriorCodigo()).thenReturn("EN_RUTA");
        when(change.getEstadoCodigo()).thenReturn("EN_CAMINO");
        when(change.getAccion()).thenReturn("CAMBIAR_ESTADO");
        when(change.getUsuarioId()).thenReturn(actorId);
        when(change.getUsuarioNombre()).thenReturn("Delivery Uno");
        when(change.getCreadoEn()).thenReturn(java.time.OffsetDateTime.now());
        autenticar("logistica@example.com", "LOGISTICA");

        var history = pedidoService.listarHistorialEstados(pedidoId);

        assertEquals(1, history.size());
        assertEquals("EN_RUTA", history.get(0).estadoAnteriorCodigo());
        assertEquals("EN_CAMINO", history.get(0).estadoCodigo());
        assertEquals(actorId, history.get(0).usuarioId());
        assertEquals("Delivery Uno", history.get(0).usuarioNombre());
        org.junit.jupiter.api.Assertions.assertNotNull(history.get(0).creadoEn());
    }

    @Test
    void armaLaListaCompletaConsultandoDetallesYPagosUnaSolaVez() {
        List<Pedido> pedidos = List.of(pedido("PED-001"), pedido("PED-002"));
        List<UUID> pedidoIds = pedidos.stream().map(Pedido::getId).toList();
        autenticar("admin@example.com", "ADMIN");
        when(pedidoRepository.findAllByOrderByCreatedAtDesc()).thenReturn(pedidos);

        List<PedidoResponse> respuestas = pedidoService.listarPedidosVisibles();

        org.junit.jupiter.api.Assertions.assertEquals(2, respuestas.size());
        verify(detallePedidoRepository).findByPedido_IdInOrderByPedido_IdAscIdAsc(pedidoIds);
        verify(pagoRepository).findByPedido_IdInOrderByPedido_IdAscCreatedAtAsc(pedidoIds);
        verify(detallePedidoRepository, never()).findByPedido_IdOrderByIdAsc(org.mockito.ArgumentMatchers.any());
        verify(pagoRepository, never()).findByPedido_IdOrderByCreatedAtAsc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void paginaDePedidosDevuelveResumenSinCargarDetallesNiPagos() {
        Pedido pedido = pedido("PED-003");
        PageRequest pageable = PageRequest.of(1, 10);
        autenticar("admin@example.com", "ADMIN");
        when(pedidoRepository.findAllByOrderByCreatedAtDesc(pageable))
            .thenReturn(new PageImpl<>(List.of(pedido), pageable, 21));

        var response = pedidoService.listarPaginaVisible(pageable);

        org.junit.jupiter.api.Assertions.assertEquals(1, response.contenido().size());
        org.junit.jupiter.api.Assertions.assertEquals("PED-003", response.contenido().get(0).codigo());
        org.junit.jupiter.api.Assertions.assertEquals(1, response.pagina());
        org.junit.jupiter.api.Assertions.assertEquals(10, response.tamano());
        org.junit.jupiter.api.Assertions.assertEquals(21, response.totalElementos());
        org.junit.jupiter.api.Assertions.assertEquals(3, response.totalPaginas());
        org.junit.jupiter.api.Assertions.assertFalse(response.ultima());
        verify(detallePedidoRepository, never())
            .findByPedido_IdInOrderByPedido_IdAscIdAsc(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void presentaEstadosDeSeguimientoConTextoClaroParaElCliente() {
        Cliente cliente = new Cliente();
        cliente.setId(UUID.randomUUID());
        cliente.setActivo(true);
        Pedido pedidoDelivery = pedido("PED-DELIVERY");
        EstadoEnvio enRuta = pedidoDelivery.getEstado();
        enRuta.setCodigo("EN_RUTA");
        enRuta.setNombre("En ruta");
        Pedido pedidoEnCamino = pedido("PED-EN-CAMINO");
        EstadoEnvio enCamino = pedidoEnCamino.getEstado();
        enCamino.setCodigo("EN_CAMINO");
        enCamino.setNombre("En camino");
        Pedido pedidoPendiente = pedido("PED-PENDIENTE");
        pedidoPendiente.getEstado().setCodigo("PENDIENTE_PAGO");
        Pedido pedidoPreparacion = pedido("PED-PREPARACION");
        pedidoPreparacion.getEstado().setCodigo("PREPARACION");
        Pedido pedidoPreparado = pedido("PED-PREPARADO");
        pedidoPreparado.getEstado().setCodigo("LISTO_DESPACHO");
        Pedido pedidoListoRecojo = pedido("PED-LISTO-RECOJO");
        pedidoListoRecojo.getEstado().setCodigo("LISTO_PARA_RECOGER");
        Pedido pedidoRecojo = pedido("PED-RECOJO");
        pedidoRecojo.setTipoEntrega("RECOJO_TIENDA");
        autenticar("cliente@example.com", "CLIENTE");
        when(clienteRepository.findByEmail("cliente@example.com")).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByCliente_IdOrderByCreatedAtDesc(cliente.getId()))
            .thenReturn(List.of(
                pedidoPendiente,
                pedidoPreparacion,
                pedidoPreparado,
                pedidoDelivery,
                pedidoEnCamino,
                pedidoListoRecojo,
                pedidoRecojo
            ));

        List<PedidoResponse> responses = pedidoService.listarMisPedidos();

        assertEquals("Pendiente", responses.get(0).estadoNombre());
        assertEquals("Pendiente", responses.get(1).estadoNombre());
        assertEquals("Preparado", responses.get(2).estadoNombre());
        assertEquals("En ruta", responses.get(3).estadoNombre());
        assertEquals("En camino", responses.get(4).estadoNombre());
        assertEquals("Listo para recoger", responses.get(5).estadoNombre());
        assertEquals("Recogido en tienda", responses.get(6).estadoNombre());
        assertEquals("EN_RUTA", responses.get(3).estadoCodigo());
        assertEquals("EN_CAMINO", responses.get(4).estadoCodigo());
        assertEquals("COMPLETADA", responses.get(6).estadoCodigo());
    }

    @Test
    void bandejaLogisticaIncluyeLosEstadosOperativosHastaEntregaFallida() {
        autenticar("logistica@example.com", "LOGISTICA");
        when(pedidoRepository.findByEstado_CodigoInOrderByCreatedAtDesc(List.of(
            "PREPARACION",
            "LISTO_DESPACHO",
            "LISTO_PARA_RECOGER",
            "EN_RUTA",
            "EN_CAMINO",
            "NO_ENTREGADA"
        ))).thenReturn(List.of());

        pedidoService.listarBandejaLogistica();

        verify(pedidoRepository).findByEstado_CodigoInOrderByCreatedAtDesc(List.of(
            "PREPARACION",
            "LISTO_DESPACHO",
            "LISTO_PARA_RECOGER",
            "EN_RUTA",
            "EN_CAMINO",
            "NO_ENTREGADA"
        ));
    }

    @Test
    void listaSoloDeliveriesActivosParaLaAsignacionLogistica() {
        Usuario delivery = new Usuario();
        delivery.setId(UUID.randomUUID());
        delivery.setNombres("Ana");
        delivery.setApellidos("Rojas");
        delivery.setTelefono("999111222");
        autenticar("logistica@example.com", "LOGISTICA");
        when(usuarioRepository.findByRol_CodigoAndActivoTrueOrderByApellidosAscNombresAsc("DELIVERY"))
            .thenReturn(List.of(delivery));

        List<DeliveryOpcionResponse> deliveries = pedidoService.listarDeliveriesActivos();

        assertEquals(List.of(new DeliveryOpcionResponse(
            delivery.getId(),
            "Ana",
            "Rojas",
            "999111222"
        )), deliveries);
    }

    @Test
    void ofreceSoloTransicionesPermitidasAlRolDeLogistica() {
        UUID pedidoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        EstadoEnvio estado = new EstadoEnvio();
        estado.setId(UUID.randomUUID());
        estado.setCodigo("PREPARACION");
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(estado);
        Usuario logistica = new Usuario();
        logistica.setId(actorId);
        logistica.setActivo(true);
        autenticar("logistica@example.com", "LOGISTICA");
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("logistica@example.com")).thenReturn(Optional.of(logistica));
        when(estadoEnvioRepository.listarCodigosDestinoTransiciones(estado.getId(), "DELIVERY"))
            .thenReturn(List.of("LISTO_DESPACHO", "EN_RUTA", "CANCELADO"));

        List<String> transitions = pedidoService.listarTransicionesDisponibles(pedidoId);

        assertEquals(List.of("LISTO_DESPACHO", "EN_RUTA", "CANCELADO"), transitions);
    }

    @Test
    void logisticaPuedeMarcarElPedidoComoEnRutaDespuesDePrepararlo() {
        UUID pedidoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        EstadoEnvio listoDespacho = new EstadoEnvio();
        listoDespacho.setId(UUID.randomUUID());
        listoDespacho.setCodigo("LISTO_DESPACHO");
        Usuario logistica = new Usuario();
        logistica.setId(actorId);
        logistica.setActivo(true);
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(listoDespacho);
        autenticar("logistica@example.com", "LOGISTICA");
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("logistica@example.com")).thenReturn(Optional.of(logistica));
        when(estadoEnvioRepository.listarCodigosDestinoTransiciones(listoDespacho.getId(), "DELIVERY"))
            .thenReturn(List.of("EN_RUTA", "CANCELADO"));

        List<String> transitions = pedidoService.listarTransicionesDisponibles(pedidoId);

        assertEquals(List.of("EN_RUTA", "CANCELADO"), transitions);
    }

    @Test
    void deliveryAsignadoPuedeVerSoloLasTransicionesDeEntregaPermitidasPorLaBase() {
        UUID pedidoId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        EstadoEnvio estado = new EstadoEnvio();
        estado.setId(UUID.randomUUID());
        estado.setCodigo("EN_RUTA");
        Usuario delivery = new Usuario();
        delivery.setId(deliveryId);
        delivery.setActivo(true);
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(estado);
        pedido.setDelivery(delivery);
        autenticar("delivery@example.com", "DELIVERY");
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("delivery@example.com")).thenReturn(Optional.of(delivery));
        when(estadoEnvioRepository.listarCodigosDestinoTransiciones(estado.getId(), "DELIVERY"))
            .thenReturn(List.of("EN_CAMINO", "COMPLETADA", "NO_ENTREGADA"));

        List<String> transitions = pedidoService.listarTransicionesDisponibles(pedidoId);

        assertEquals(List.of("EN_CAMINO", "COMPLETADA", "NO_ENTREGADA"), transitions);
    }

    @Test
    void logisticaDespachaReservasAlMarcarPedidoDeliveryListoParaDespacho() {
        UUID pedidoId = UUID.randomUUID();
        UUID logisticaId = UUID.randomUUID();
        EstadoEnvio listoDespacho = new EstadoEnvio();
        listoDespacho.setId(UUID.randomUUID());
        listoDespacho.setCodigo("LISTO_DESPACHO");
        EstadoEnvio preparacion = new EstadoEnvio();
        preparacion.setId(UUID.randomUUID());
        preparacion.setCodigo("PREPARACION");
        Usuario logistica = new Usuario();
        logistica.setId(logisticaId);
        logistica.setActivo(true);
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCodigo("PED-DELIVERY");
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(preparacion);
        pedido.setTieneReserva(true);
        autenticar("logistica@example.com", "LOGISTICA");
        when(usuarioRepository.findByEmail("logistica@example.com")).thenReturn(Optional.of(logistica));
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(estadoEnvioRepository.findByCodigo("LISTO_DESPACHO")).thenReturn(Optional.of(listoDespacho));
        when(estadoEnvioRepository.existeTransicion(preparacion.getId(), listoDespacho.getId(), "DELIVERY"))
            .thenReturn(true);
        when(ventaOperacionRepository.despacharReservas(pedidoId, logisticaId)).thenReturn(true);

        PedidoResponse response = pedidoService.cambiarEstado(
            pedidoId,
            new CambiarEstadoPedidoRequest("LISTO_DESPACHO")
        );

        assertEquals("LISTO_DESPACHO", response.estadoCodigo());
        verify(ventaOperacionRepository).despacharReservas(pedidoId, logisticaId);
    }

    @Test
    void deliveryAvanzaDeEnRutaAEnCaminoSinVolverADespacharStockYaDescontado() {
        UUID pedidoId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        EstadoEnvio enRuta = new EstadoEnvio();
        enRuta.setId(UUID.randomUUID());
        enRuta.setCodigo("EN_RUTA");
        EstadoEnvio enCamino = new EstadoEnvio();
        enCamino.setId(UUID.randomUUID());
        enCamino.setCodigo("EN_CAMINO");
        enCamino.setNombre("En camino");
        Usuario delivery = new Usuario();
        delivery.setId(deliveryId);
        delivery.setActivo(true);
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCodigo("PED-DELIVERY");
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(enRuta);
        pedido.setDelivery(delivery);
        pedido.setTieneReserva(false);
        autenticar("delivery@example.com", "DELIVERY");
        when(usuarioRepository.findByEmail("delivery@example.com")).thenReturn(Optional.of(delivery));
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(estadoEnvioRepository.findByCodigo("EN_CAMINO")).thenReturn(Optional.of(enCamino));
        when(estadoEnvioRepository.existeTransicion(enRuta.getId(), enCamino.getId(), "DELIVERY"))
            .thenReturn(true);

        PedidoResponse response = pedidoService.cambiarEstado(
            pedidoId,
            new CambiarEstadoPedidoRequest("EN_CAMINO")
        );

        assertEquals("EN_CAMINO", response.estadoCodigo());
        verify(ventaOperacionRepository, never()).despacharReservas(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void logisticaDespachaReservasAlMarcarPedidoDeRecojoListoParaRecoger() {
        UUID pedidoId = UUID.randomUUID();
        UUID logisticaId = UUID.randomUUID();
        EstadoEnvio preparacion = new EstadoEnvio();
        preparacion.setId(UUID.randomUUID());
        preparacion.setCodigo("PREPARACION");
        EstadoEnvio listoParaRecoger = new EstadoEnvio();
        listoParaRecoger.setId(UUID.randomUUID());
        listoParaRecoger.setCodigo("LISTO_PARA_RECOGER");
        Usuario logistica = new Usuario();
        logistica.setId(logisticaId);
        logistica.setActivo(true);
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCodigo("PED-RECOJO");
        pedido.setTipoEntrega("RECOJO_TIENDA");
        pedido.setEstado(preparacion);
        pedido.setTieneReserva(true);
        autenticar("logistica@example.com", "LOGISTICA");
        when(usuarioRepository.findByEmail("logistica@example.com")).thenReturn(Optional.of(logistica));
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(estadoEnvioRepository.findByCodigo("LISTO_PARA_RECOGER")).thenReturn(Optional.of(listoParaRecoger));
        when(estadoEnvioRepository.existeTransicion(
            preparacion.getId(),
            listoParaRecoger.getId(),
            "RECOJO_TIENDA"
        )).thenReturn(true);
        when(ventaOperacionRepository.despacharReservas(pedidoId, logisticaId)).thenReturn(true);

        PedidoResponse response = pedidoService.cambiarEstado(
            pedidoId,
            new CambiarEstadoPedidoRequest("LISTO_PARA_RECOGER")
        );

        assertEquals("LISTO_PARA_RECOGER", response.estadoCodigo());
        verify(ventaOperacionRepository).despacharReservas(pedidoId, logisticaId);
    }

    @Test
    void noPermiteAsignarPedidoAntesDeQueLogisticaLoMarqueListoParaDespacho() {
        UUID pedidoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Usuario logistica = new Usuario();
        logistica.setId(actorId);
        logistica.setActivo(true);
        EstadoEnvio estado = new EstadoEnvio();
        estado.setCodigo("PREPARACION");
        Pedido pedido = new Pedido();
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(estado);
        autenticar("logistica@example.com", "LOGISTICA");
        when(usuarioRepository.findByEmail("logistica@example.com")).thenReturn(Optional.of(logistica));
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        assertThrows(
            BusinessException.class,
            () -> pedidoService.asignarDelivery(pedidoId, new AsignarDeliveryRequest(UUID.randomUUID()))
        );

        verify(usuarioRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(pedidoRepository, never()).saveAndFlush(pedido);
    }

    @Test
    void noPermiteAsignarOtroDeliverySiElPedidoYaTieneUno() {
        UUID pedidoId = UUID.randomUUID();
        Usuario logistica = new Usuario();
        logistica.setActivo(true);
        Usuario deliveryActual = new Usuario();
        deliveryActual.setId(UUID.randomUUID());
        EstadoEnvio estado = new EstadoEnvio();
        estado.setCodigo("LISTO_DESPACHO");
        Pedido pedido = new Pedido();
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(estado);
        pedido.setDelivery(deliveryActual);
        autenticar("logistica@example.com", "LOGISTICA");
        when(usuarioRepository.findByEmail("logistica@example.com")).thenReturn(Optional.of(logistica));
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> pedidoService.asignarDelivery(pedidoId, new AsignarDeliveryRequest(UUID.randomUUID()))
        );

        assertEquals("El pedido ya tiene un delivery asignado", exception.getMessage());
        verify(usuarioRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(pedidoRepository, never()).saveAndFlush(pedido);
    }

    @Test
    void alAsignarDeliveryRegistraLaRelacionConLogisticaYConDelivery() {
        UUID pedidoId = UUID.randomUUID();
        UUID logisticaId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        Usuario logistica = new Usuario();
        logistica.setId(logisticaId);
        logistica.setNombres("Laura");
        logistica.setApellidos("Logística");
        logistica.setActivo(true);
        Usuario delivery = new Usuario();
        delivery.setId(deliveryId);
        delivery.setNombres("Diego");
        delivery.setApellidos("Reparto");
        delivery.setActivo(true);
        Rol deliveryRole = new Rol();
        deliveryRole.setCodigo("DELIVERY");
        delivery.setRol(deliveryRole);
        EstadoEnvio estado = new EstadoEnvio();
        estado.setId(UUID.randomUUID());
        estado.setCodigo("LISTO_DESPACHO");
        estado.setNombre("Listo para despacho");
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setCodigo("PED-000001");
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(estado);
        autenticar("logistica@example.com", "LOGISTICA");
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("logistica@example.com")).thenReturn(Optional.of(logistica));
        when(usuarioRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(detallePedidoRepository.findByPedido_IdInOrderByPedido_IdAscIdAsc(List.of(pedidoId)))
            .thenReturn(List.of());
        when(pagoRepository.findByPedido_IdInOrderByPedido_IdAscCreatedAtAsc(List.of(pedidoId)))
            .thenReturn(List.of());

        PedidoResponse response = pedidoService.asignarDelivery(
            pedidoId,
            new AsignarDeliveryRequest(deliveryId)
        );

        assertEquals(logisticaId, response.logisticaId());
        assertEquals("Laura Logística", response.logisticaNombre());
        assertEquals(deliveryId, response.deliveryId());
        assertEquals("Diego Reparto", response.deliveryNombre());
        verify(pedidoRepository).saveAndFlush(pedido);
    }

    private void autenticar(String email, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                email,
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
            )
        );
    }

    private Pedido pedido(String codigo) {
        EstadoEnvio estado = new EstadoEnvio();
        estado.setCodigo("COMPLETADA");
        estado.setNombre("Completada");
        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID());
        pedido.setCodigo(codigo);
        pedido.setCanal("WEB");
        pedido.setTipoEntrega("DELIVERY");
        pedido.setEstado(estado);
        pedido.setTipoComprobante("BOLETA");
        pedido.setSubtotal(new BigDecimal("20.00"));
        pedido.setDescuentoCupon(BigDecimal.ZERO);
        pedido.setCostoEnvio(BigDecimal.ZERO);
        pedido.setIgv(new BigDecimal("3.60"));
        pedido.setTotal(new BigDecimal("23.60"));
        pedido.setTieneReserva(true);
        pedido.setDespachado(false);
        return pedido;
    }
}
