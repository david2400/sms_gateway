package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

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
@Table(name = "SmsEnvioIntento", indexes = @Index(name = "ix_sms_intento_envio", columnList = "envio_id"))
@Getter
@Setter
@NoArgsConstructor
public class SmsEnvioIntentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_envio_intento")
    private Long idSmsEnvioIntento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envio_id", nullable = false)
    private SmsEnvioEntity envio;

    @Column(name = "paso_orden", nullable = false)
    private Integer pasoOrden;

    /** Número de intento dentro de la cuenta (1..max_intentos). */
    @Column(name = "nro_intento", nullable = false)
    private Integer nroIntento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private SmsProveedorEntity proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private SmsCuentaEntity cuenta;

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
