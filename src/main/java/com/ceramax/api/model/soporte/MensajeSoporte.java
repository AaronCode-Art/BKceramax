package com.ceramax.api.model.soporte;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mensajesoporte")
@Getter
@Setter
@NoArgsConstructor
public class MensajeSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_chat", nullable = false)
    private UUID idChat;

    @Column(name = "id_cliente")
    private UUID idCliente;

    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "tipo_remitente", nullable = false, length = 10)
    private String tipoRemitente;

    @Column(name = "contenido", nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
