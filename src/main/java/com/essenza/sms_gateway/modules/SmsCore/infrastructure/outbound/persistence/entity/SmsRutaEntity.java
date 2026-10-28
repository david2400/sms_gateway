package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.persistence.entity;

import com.essenza.sms_gateway.shared.common.domain.entity.AuditInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ruta (filas {@code canal = 'SMS'} de {@code CanalRuta}) = (proveedor, tipo de mensaje).
 * {@code proveedor == null} es la ruta del endpoint central.
 */
@Entity
@SQLRestriction("canal = 'SMS'")
@Table(name = "CanalRuta")
@Getter
@Setter
@NoArgsConstructor
public class SmsRutaEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long idSmsRuta;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proveedor_id")
    private SmsProveedorEntity proveedor;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_mensaje_id", nullable = false)
    private SmsTipoMensajeEntity tipoMensaje;

    /** Canal de la configuración (SMS | EMAIL); este gateway solo ve las filas de SMS. Lo escribe notificacion. */
    @Column(name = "canal", length = 10, nullable = false, insertable = false, updatable = false)
    private String canalAdmin;
}
