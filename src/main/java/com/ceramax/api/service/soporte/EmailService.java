package com.ceramax.api.service.soporte;

import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailDeliveryService emailDeliveryService;

    public EmailService(EmailDeliveryService emailDeliveryService) {
        this.emailDeliveryService = emailDeliveryService;
    }

    @Async
    public CompletableFuture<Void> enviarHtml(String destinatario, String asunto, String contenidoHtml) {
        emailDeliveryService.enviarHtml(destinatario, asunto, contenidoHtml);
        return CompletableFuture.completedFuture(null);
    }
}
