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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Servicio habilitado de un proveedor (filas {@code canal = 'SMS'} de {@code CanalProveedorServicio}) = (proveedor, tipo de mensaje).
 * Si no existe la fila, el proveedor no atiende ese tipo.
 */
@Entity
@SQLRestriction("canal = 'SMS'")
@Table(name = "CanalProveedorServicio",
        uniqueConstraints = @UniqueConstraint(name = "uq_canal_proveedor_servicio", columnNames = {"proveedor_id", "tipo_mensaje_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SmsProveedorServicioEntity extends AuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor_servicio")
    private Long idSmsProveedorServicio;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private SmsProveedorEntity proveedor;

    // EAGER: Hibernate 6.4 no permite LAZY hacia una entidad con @SoftDelete (AuditInfo)

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_mensaje_id", nullable = false)
    private SmsTipoMensajeEntity tipoMensaje;

    /** Canal de la configuración (SMS | EMAIL); este gateway solo ve las filas de SMS. Lo escribe notificacion. */
    @Column(name = "canal", length = 10, nullable = false, insertable = false, updatable = false)
    private String canalAdmin;
}
