package com.ceramax.api.model.resena;

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
@Table(name = "resenaimagen")
@Getter
@Setter
@NoArgsConstructor
public class ResenaImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_resena", nullable = false)
    private UUID idResena;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "public_id", length = 200)
    private String publicId;

    @Column(name = "orden", nullable = false)
    private Short orden = 0;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
