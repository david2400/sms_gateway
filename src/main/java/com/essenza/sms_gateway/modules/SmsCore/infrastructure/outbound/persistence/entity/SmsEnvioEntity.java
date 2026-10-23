package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Log de envíos ({@code sms_envio}): un registro por SMS solicitado,
 * con el proveedor y la cuenta con los que finalmente se envió.
 */
@Entity
@Table(name = "SmsEnvio", indexes = {
        @Index(name = "ix_sms_envio_destino", columnList = "destino, fecha_creacion"),
        @Index(name = "ix_sms_envio_estado", columnList = "estado, fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
public class SmsEnvioEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_envio")
    private Long idSmsEnvio;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    /** Único cuando no es null (índice filtrado creado por el script SQL). */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "canal", length = 20, nullable = false)
    private String canal;

    @Column(name = "proveedor_solicitado", length = 30)
    private String proveedorSolicitado;

    @Column(name = "tipo_mensaje", length = 30, nullable = false)
    private String tipoMensaje;

    @Column(name = "destino", length = 20, nullable = false)
    private String destino;

    @Column(name = "mensaje", length = 1600, nullable = false)
    private String mensaje;

    /** Plantilla con la que se generó el mensaje (null = texto directo). */
    @Column(name = "plantilla_codigo", length = 50)
    private String plantillaCodigo;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_final_id")
    private SmsProveedorEntity proveedorFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_final_id")
    private SmsCuentaEntity cuentaFinal;

    @Column(name = "http_status_final")
    private Integer httpStatusFinal;

    @Column(name = "total_intentos", nullable = false)
    private Integer totalIntentos = 0;

    @Column(name = "codigo_error", length = 50)
    private String codigoError;

    @Column(name = "detalle_error", length = 1000)
    private String detalleError;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;
}
