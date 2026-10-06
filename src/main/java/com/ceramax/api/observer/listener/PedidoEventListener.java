package com.ceramax.api.observer.listener;

import com.ceramax.api.observer.event.PedidoCreadoEvent;
import com.ceramax.api.observer.event.PedidoEstadoCambiadoEvent;
import com.ceramax.api.observer.event.PedidoNotificacion;
import com.ceramax.api.service.soporte.EmailService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

@Component
public class PedidoEventListener {

    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    public PedidoEventListener(EmailService emailService, SimpMessagingTemplate messagingTemplate) {
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCreacion(PedidoCreadoEvent event) {
        PedidoNotificacion notificacion = new PedidoNotificacion(
            event.pedidoId(),
            event.codigoPedido(),
            event.canal(),
            event.estadoCodigo(),
            event.estadoNombre()
        );
        enviarPorSocket(event.clienteEmail(), notificacion);
        enviarCorreo(
            event.clienteEmail(),
            "Pedido " + event.codigoPedido() + " registrado",
            "<p>Tu pedido <strong>" + HtmlUtils.htmlEscape(event.codigoPedido())
                + "</strong> fue registrado. Estado: "
                + HtmlUtils.htmlEscape(event.estadoNombre()) + ".</p>"
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCambioEstado(PedidoEstadoCambiadoEvent event) {
        PedidoNotificacion notificacion = new PedidoNotificacion(
            event.pedidoId(),
            event.codigoPedido(),
            null,
            event.estadoCodigo(),
            event.estadoNombre()
        );
        enviarPorSocket(event.clienteEmail(), notificacion);
        enviarPorSocket(event.deliveryEmail(), notificacion);
        String codigoSeguro = HtmlUtils.htmlEscape(event.codigoPedido());
        String estadoSeguro = HtmlUtils.htmlEscape(event.estadoNombre());
        enviarCorreo(
            event.clienteEmail(),
            "Actualización del pedido " + event.codigoPedido(),
            "<p>El pedido <strong>" + codigoSeguro + "</strong> ahora está: "
                + estadoSeguro + ".</p>"
        );
    }

    private void enviarPorSocket(String destinatario, PedidoNotificacion notificacion) {
        if (destinatario != null && !destinatario.isBlank()) {
            messagingTemplate.convertAndSendToUser(destinatario, "/queue/pedidos", notificacion);
        }
    }

    private void enviarCorreo(String destinatario, String asunto, String contenido) {
        if (destinatario != null && !destinatario.isBlank()) {
            emailService.enviarHtml(destinatario, asunto, contenido);
        }
    }
}
