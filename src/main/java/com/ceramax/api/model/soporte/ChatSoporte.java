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
@Table(name = "chatsoporte")
@Getter
@Setter
@NoArgsConstructor
public class ChatSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20, insertable = false, updatable = false)
    private String codigo;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(name = "id_pedido")
    private UUID idPedido;

    @Column(name = "id_usuario_creador")
    private UUID idUsuarioCreador;

    @Column(name = "id_usuario_asignado")
    private UUID idUsuarioAsignado;

    @Column(name = "asunto", length = 150)
    private String asunto;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "ABIERTO";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
