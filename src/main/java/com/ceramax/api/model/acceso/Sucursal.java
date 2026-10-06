package com.ceramax.api.model.acceso;

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
@Table(name = "sucursal")
@Getter
@Setter
@NoArgsConstructor
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20, insertable = false, updatable = false)
    private String codigo;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "departamento", nullable = false, length = 60)
    private String departamento;

    @Column(name = "provincia", nullable = false, length = 60)
    private String provincia;

    @Column(name = "distrito", nullable = false, length = 60)
    private String distrito;

    @Column(name = "direccion", nullable = false, length = 200)
    private String direccion;

    @Column(name = "referencia", length = 250)
    private String referencia;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "id_ubigeo", length = 6)
    private String idUbigeo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
