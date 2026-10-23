package com.ceramax.api.observer.listener;

import com.ceramax.api.observer.event.CodigoRecuperacionContrasenaEvent;
import com.ceramax.api.service.soporte.EmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CodigoRecuperacionContrasenaEventListener {

    private final EmailService emailService;

    public CodigoRecuperacionContrasenaEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarCodigo(CodigoRecuperacionContrasenaEvent event) {
        String contenido = """
            <div style="font-family:Arial,sans-serif;color:#203d32;max-width:520px;margin:auto">
              <h2>Restablece tu contraseña</h2>
              <p>Hola %s. Usa este código para crear una contraseña nueva:</p>
              <p style="font-size:30px;font-weight:bold;letter-spacing:8px">%s</p>
              <p>El código vence en 10 minutos y solo puede usarse una vez.</p>
              <p>Si no solicitaste este cambio, puedes ignorar este correo.</p>
              <p>Equipo Cerámax</p>
            </div>
            """.formatted(event.nombre(), event.codigo());
        emailService.enviarHtml(
            event.destinatario(),
            "Código para restablecer tu contraseña de Cerámax",
            contenido
        );
    }
}
