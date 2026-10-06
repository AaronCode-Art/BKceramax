package com.ceramax.api.model.acceso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cliente_documento", columnNames = { "tipo_documento", "numero_documento" })
})
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20, insertable = false, updatable = false)
    private String codigo;

    @Column(name = "tipo_documento", nullable = false, length = 12)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "email", unique = true, length = 120)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "departamento", length = 60)
    private String departamento;

    @Column(name = "provincia", length = 60)
    private String provincia;

    @Column(name = "distrito", length = 60)
    private String distrito;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "referencia", length = 250)
    private String referencia;

    @Column(name = "id_ubigeo", length = 6)
    private String idUbigeo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
