package com.ceramax.api.observer.listener;

import com.ceramax.api.observer.event.MensajeChatEvent;
import com.ceramax.api.observer.event.PedidoCreadoEvent;
import com.ceramax.api.observer.event.PedidoEstadoCambiadoEvent;
import com.ceramax.api.observer.event.TrasladoRecibidoEvent;
import com.ceramax.api.service.auditoria.AuditoriaService;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditoriaEventListener {

    private final AuditoriaService auditoriaService;

    public AuditoriaEventListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void auditarPedidoCreado(PedidoCreadoEvent event) {
        auditoriaService.registrar(
            event.usuarioActorId(),
            "CREAR_PEDIDO",
            "pedido",
            event.pedidoId(),
            "Pedido " + event.codigoPedido() + " creado por canal " + event.canal(),
            null,
            Map.of(
                "codigo", event.codigoPedido(),
                "canal", event.canal(),
                "estado", event.estadoCodigo()
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void auditarPedidoActualizado(PedidoEstadoCambiadoEvent event) {
        auditoriaService.registrar(
            event.usuarioActorId(),
            event.accion(),
            "pedido",
            event.pedidoId(),
            "Pedido " + event.codigoPedido() + ": " + event.accion(),
            Map.of("estado", event.estadoAnteriorCodigo()),
            Map.of(
                "estado", event.estadoCodigo(),
                "accion", event.accion()
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void auditarMensajeChat(MensajeChatEvent event) {
        String tabla = "soporte".equals(event.tipoChat()) ? "mensajesoporte" : "chatmensajeinterno";
        String accion = "soporte".equals(event.tipoChat())
            ? "ENVIAR_MENSAJE_SOPORTE"
            : "ENVIAR_MENSAJE_INTERNO";
        UUID actorId = "PERSONAL".equals(event.mensaje().tipoRemitente())
            ? event.mensaje().remitenteId()
            : null;
        auditoriaService.registrar(
            actorId,
            accion,
            tabla,
            event.mensaje().id(),
            "Mensaje registrado en chat " + event.chatId(),
            null,
            Map.of(
                "chatId", event.chatId(),
                "tipoRemitente", event.mensaje().tipoRemitente()
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void auditarTrasladoRecibido(TrasladoRecibidoEvent event) {
        auditoriaService.registrar(
            event.usuarioActorId(),
            "RECIBIR_TRASLADO",
            "traslado_inventario",
            event.trasladoId(),
            "Traslado " + event.codigo() + " recibido en " + event.almacenDestinoNombre(),
            Map.of("estado", "EN_TRANSITO"),
            Map.of(
                "estado", "RECIBIDO",
                "productoId", event.productoId(),
                "cantidad", event.cantidad()
            )
        );
    }
}
