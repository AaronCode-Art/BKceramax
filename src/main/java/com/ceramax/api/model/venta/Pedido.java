package com.ceramax.api.model.venta;

import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Sucursal;
import com.ceramax.api.model.acceso.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20, insertable = false, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor")
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_delivery")
    private Usuario delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_logistica")
    private Usuario logistica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoEnvio estado;

    @Column(name = "canal", nullable = false, length = 12)
    private String canal;

    @Column(name = "tipo_entrega", nullable = false, length = 15)
    private String tipoEntrega;

    @Column(name = "cliente_tipo_documento", length = 12)
    private String clienteTipoDocumento;

    @Column(name = "cliente_numero_documento", length = 20)
    private String clienteNumeroDocumento;

    @Column(name = "cliente_nombre", length = 170)
    private String clienteNombre;

    @Column(name = "cliente_email", length = 120)
    private String clienteEmail;

    @Column(name = "cliente_telefono", length = 20)
    private String clienteTelefono;

    @Column(name = "envio_departamento", length = 60)
    private String envioDepartamento;

    @Column(name = "envio_provincia", length = 60)
    private String envioProvincia;

    @Column(name = "envio_distrito", length = 60)
    private String envioDistrito;

    @Column(name = "envio_direccion", length = 200)
    private String envioDireccion;

    @Column(name = "envio_codigo_postal", length = 10)
    private String envioCodigoPostal;

    @Column(name = "envio_referencia", length = 250)
    private String envioReferencia;

    @Column(name = "envio_id_ubigeo", length = 6)
    private String envioIdUbigeo;

    @Column(name = "sucursal_nombre", length = 80)
    private String sucursalNombre;

    @Column(name = "sucursal_direccion", length = 300)
    private String sucursalDireccion;

    @Column(name = "vendedor_nombre", length = 170)
    private String vendedorNombre;

    @Column(name = "delivery_nombre", length = 170)
    private String deliveryNombre;

    @Column(name = "logistica_nombre", length = 170)
    private String logisticaNombre;

    @Column(name = "costo_envio", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEnvio;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento_cupon", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoCupon = BigDecimal.ZERO;

    @Column(name = "igv", nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "tipo_comprobante", nullable = false, length = 10)
    private String tipoComprobante;

    @Column(name = "serie", length = 10)
    private String serie;

    @Column(name = "numero_comprobante", length = 20)
    private String numeroComprobante;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(name = "stock_pendiente", nullable = false)
    private Boolean stockPendiente = false;

    @Column(name = "tiene_reserva", nullable = false)
    private Boolean tieneReserva = false;

    @Column(name = "despachado", nullable = false)
    private Boolean despachado = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
