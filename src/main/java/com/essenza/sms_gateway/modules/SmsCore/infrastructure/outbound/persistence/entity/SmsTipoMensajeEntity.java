package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

import jakarta.persistence.UniqueConstraint;
import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de tipos de mensaje (filas {@code canal = 'SMS'} de {@code CanalTipoMensaje}) vista por el motor.
 * {@code CanalRuta.tipo_mensaje_id} referencia esta tabla; el motor filtra por {@code codigo}
 * (el nombre del {@code MessageType} del request).
 */
@Entity
@SQLRestriction("canal = 'SMS'")
@Table(name = "CanalTipoMensaje",
        uniqueConstraints = @UniqueConstraint(name = "uq_canal_tipo_mensaje", columnNames = {"canal", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class SmsTipoMensajeEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_mensaje")
    private Long idSmsTipoMensaje;

    @Column(name = "codigo", length = 30, nullable = false)
    private String codigo;

    /** Solo lo administra notificacion (AdminCanales); se mapea para que el esquema quede completo con hbm2ddl. */
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /** SMS | WHATSAPP (columna canal_mensaje). */
    @Column(name = "canal_mensaje", length = 20, nullable = false)
    private String canal;

    @Column(name = "otp")
    private Boolean otp;

    @Column(name = "masivo", nullable = false)
    private Boolean masivo;

    /** Canal de la configuración (SMS | EMAIL); este gateway solo ve las filas de SMS. Lo escribe notificacion. */
    @Column(name = "canal", length = 10, nullable = false, insertable = false, updatable = false)
    private String canalAdmin;
}
