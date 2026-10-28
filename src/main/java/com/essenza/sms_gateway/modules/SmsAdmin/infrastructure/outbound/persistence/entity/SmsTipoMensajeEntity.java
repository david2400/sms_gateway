package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de tipos de mensaje ({@code sms_tipo_mensaje}) vista por el motor.
 * {@code sms_ruta.tipo_mensaje_id} referencia esta tabla; el motor filtra por {@code codigo}
 * (el nombre del {@code MessageType} del request).
 */
@Entity
@Table(name = "SmsTipoMensaje")
@Getter
@Setter
@NoArgsConstructor
public class SmsTipoMensajeEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sms_tipo_mensaje")
    private Long idSmsTipoMensaje;

    @Column(name = "codigo", length = 30, nullable = false, unique = true)
    private String codigo;

    /** Solo lo administra notificacion (AdminCanales); se mapea para que el esquema quede completo con hbm2ddl. */
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "canal", length = 20, nullable = false)
    private String canal;

    @Column(name = "otp", nullable = false)
    private Boolean otp;

    @Column(name = "masivo", nullable = false)
    private Boolean masivo;

}
