package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Intento de envío contra una cuenta ({@code sms_envio_intento}).
 */
@Entity
@Table(name = "SmsEnvioIntento", indexes = @Index(name = "ix_sms_intento_envio", columnList = "sms_envio_id"))
@Getter
@Setter
@NoArgsConstructor
public class SmsEnvioIntentoEntity  extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_envio_intento")
    private Long idSmsEnvioIntento;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sms_envio_id", nullable = false)
    private SmsEnvioEntity envio;

    @Column(name = "paso_orden", nullable = false)
    private Integer pasoOrden;

    /** Número de intento dentro de la cuenta (1..max_intentos). */
    @Column(name = "nro_intento", nullable = false)
    private Integer nroIntento;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "canal_proveedor_id", nullable = false)
    private CanalProveedorEntity canalProveedor;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "canal_cuenta_id", nullable = false)
    private CanalCuentaEntity canalCuenta;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso;

    /** Null si no hubo respuesta (timeout / error de red). */
    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "detalle", length = 1000)
    private String detalle;

    @Column(name = "duracion_ms", nullable = false)
    private Integer duracionMs;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
